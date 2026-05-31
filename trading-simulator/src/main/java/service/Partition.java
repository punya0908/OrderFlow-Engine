package service;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import engine.MatchingEngine;
import engine.OrderBook;
import model.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

public class Partition implements EventHandler<OrderEvent> {

    private final int partitionId;
    private final OrderBook orderBook = new OrderBook();
    private final MatchingEngine matchingEngine = new MatchingEngine();
    private final Queue<Trade> globalTradeHistory;
    private final List<MarketDataListener> listeners;
    private final PersistenceService persistenceService;

    private final Disruptor<OrderEvent> disruptor;
    private final RingBuffer<OrderEvent> ringBuffer;

    // Latency Metrics (Nanoseconds) for this partition
    private final LongAdder totalCount = new LongAdder();
    private final LongAdder totalLatencyNs = new LongAdder();
    private final LongAccumulator maxLatencyNs = new LongAccumulator(Long::max, 0L);
    private final LongAccumulator minLatencyNs = new LongAccumulator(Long::min, Long.MAX_VALUE);

    private final LongAdder totalMatchNs = new LongAdder();
    private final LongAccumulator maxMatchNs = new LongAccumulator(Long::max, 0L);
    private final LongAccumulator minMatchNs = new LongAccumulator(Long::min, Long.MAX_VALUE);

    private final LongAdder totalQueueNs = new LongAdder();
    private final LongAccumulator maxQueueNs = new LongAccumulator(Long::max, 0L);
    private final LongAccumulator minQueueNs = new LongAccumulator(Long::min, Long.MAX_VALUE);

    private final Queue<Long> rollingTotalLatencies;
    private final AtomicInteger rollingTotalSize;
    private final Queue<Long> rollingMatchLatencies;
    private final AtomicInteger rollingMatchSize;

    // Temporary list to accumulate trades executed in the current batch for batch notifications
    private final List<Trade> batchTrades = new ArrayList<>();

    public Partition(
            int partitionId,
            Queue<Trade> globalTradeHistory,
            List<MarketDataListener> listeners,
            Queue<Long> rollingTotalLatencies,
            AtomicInteger rollingTotalSize,
            Queue<Long> rollingMatchLatencies,
            AtomicInteger rollingMatchSize,
            PersistenceService persistenceService
    ) {
        this.partitionId = partitionId;
        this.globalTradeHistory = globalTradeHistory;
        this.listeners = listeners;
        this.rollingTotalLatencies = rollingTotalLatencies;
        this.rollingTotalSize = rollingTotalSize;
        this.rollingMatchLatencies = rollingMatchLatencies;
        this.rollingMatchSize = rollingMatchSize;
        this.persistenceService = persistenceService;

        // Buffer size must be a power of 2
        int bufferSize = 4096;
        this.disruptor = new Disruptor<>(
                OrderEvent::new,
                bufferSize,
                DaemonThreadFactory.INSTANCE,
                com.lmax.disruptor.dsl.ProducerType.MULTI,
                new com.lmax.disruptor.YieldingWaitStrategy()
        );
        this.disruptor.handleEventsWith(this);
        this.ringBuffer = this.disruptor.start();
    }

    public RingBuffer<OrderEvent> getRingBuffer() {
        return ringBuffer;
    }

    public OrderBook getOrderBook() {
        return orderBook;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) throws Exception {
        try {
            if (event.getType() == OrderEvent.Type.SUBMIT) {
                Order order = event.getOrder();
                long startNs = order.getTimestamp();
                long dequeueTimeNs = System.nanoTime();
                long queueWaitTimeNs = dequeueTimeNs - startNs;

                long matchStartNs = System.nanoTime();
                // Match order in the single-thread environment
                List<Trade> executedTrades = matchingEngine.match(order, orderBook, () -> System.nanoTime());
                long matchEndNs = System.nanoTime();

                long matchTimeNs = matchEndNs - matchStartNs;
                long totalLatencyNs = matchEndNs - startNs;

                globalTradeHistory.addAll(executedTrades);
                batchTrades.addAll(executedTrades);

                recordLatency(queueWaitTimeNs, matchTimeNs, totalLatencyNs);

                // Asynchronously save to DB
                if (persistenceService != null) {
                    persistenceService.persistOrderPlacement(order);
                    if (!executedTrades.isEmpty()) {
                        persistenceService.persistTrades(executedTrades);
                    }
                }

                // Resolve the future for the caller
                event.getSubmitFuture().complete(new OrderResult(order, executedTrades, totalLatencyNs));

            } else if (event.getType() == OrderEvent.Type.CANCEL) {
                long startNs = System.nanoTime();
                boolean canceled = orderBook.cancelOrder(event.getCancelOrderId());
                long latencyNs = System.nanoTime() - startNs;
                recordLatency(0L, latencyNs, latencyNs);

                // Asynchronously save cancel to DB
                if (persistenceService != null) {
                    persistenceService.persistOrderCancel(event.getCancelOrderId());
                }

                event.getCancelFuture().complete(canceled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Batch Processing optimization: only notify listeners at the end of the batch
            if (endOfBatch) {
                notifyListeners(new ArrayList<>(batchTrades));
                batchTrades.clear();
            }
            event.clear();
        }
    }

    private void recordLatency(long queueWaitTimeNs, long matchTimeNs, long totalLatencyNs) {
        totalCount.increment();
        
        this.totalLatencyNs.add(totalLatencyNs);
        this.maxLatencyNs.accumulate(totalLatencyNs);
        this.minLatencyNs.accumulate(totalLatencyNs);

        this.totalMatchNs.add(matchTimeNs);
        this.maxMatchNs.accumulate(matchTimeNs);
        this.minMatchNs.accumulate(matchTimeNs);

        this.totalQueueNs.add(queueWaitTimeNs);
        this.maxQueueNs.accumulate(queueWaitTimeNs);
        this.minQueueNs.accumulate(queueWaitTimeNs);

        // Update rolling total list with O(1) size check
        rollingTotalLatencies.offer(totalLatencyNs);
        if (rollingTotalSize.incrementAndGet() > 10000) {
            rollingTotalLatencies.poll();
            rollingTotalSize.decrementAndGet();
        }

        // Update rolling match list with O(1) size check
        rollingMatchLatencies.offer(matchTimeNs);
        if (rollingMatchSize.incrementAndGet() > 10000) {
            rollingMatchLatencies.poll();
            rollingMatchSize.decrementAndGet();
        }
    }

    private void notifyListeners(List<Trade> trades) {
        if (!listeners.isEmpty()) {
            if (!trades.isEmpty()) {
                listeners.forEach(l -> l.onTrades(trades));
            }
            listeners.forEach(l -> l.onBookUpdate(orderBook));
        }
    }

    public long getTotalCount() {
        return totalCount.sum();
    }

    public long getTotalLatencyNs() {
        return totalLatencyNs.sum();
    }

    public long getMinLatencyNs() {
        return minLatencyNs.get();
    }

    public long getMaxLatencyNs() {
        return maxLatencyNs.get();
    }

    public long getTotalMatchNs() {
        return totalMatchNs.sum();
    }

    public long getMinMatchNs() {
        return minMatchNs.get();
    }

    public long getMaxMatchNs() {
        return maxMatchNs.get();
    }

    public long getTotalQueueNs() {
        return totalQueueNs.sum();
    }

    public long getMinQueueNs() {
        return minQueueNs.get();
    }

    public long getMaxQueueNs() {
        return maxQueueNs.get();
    }

    public void clear() {
        orderBook.getBids().clear();
        orderBook.getAsks().clear();
        orderBook.getOrderLookup().clear();
        totalCount.reset();
        totalLatencyNs.reset();
        maxLatencyNs.reset();
        minLatencyNs.accumulate(Long.MAX_VALUE);
        totalMatchNs.reset();
        maxMatchNs.reset();
        minMatchNs.accumulate(Long.MAX_VALUE);
        totalQueueNs.reset();
        maxQueueNs.reset();
        minQueueNs.accumulate(Long.MAX_VALUE);
        batchTrades.clear();
    }

    public void shutdown() {
        disruptor.shutdown();
    }
}
