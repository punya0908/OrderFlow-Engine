package service;

import com.lmax.disruptor.RingBuffer;
import engine.OrderBook;
import model.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TradingService {

    private final Partition[] partitions;
    private final AtomicLong orderIdGenerator = new AtomicLong(1);

    // Global shared trade history and listeners
    private final Queue<Trade> tradeHistory = new ConcurrentLinkedQueue<>();
    private final List<MarketDataListener> listeners = new CopyOnWriteArrayList<>();

    // Global performance metrics across partitions
    private final LongAdder totalCount = new LongAdder();
    private static final int MAX_ROLLING_LATENCIES = 10000;
    
    private final Queue<Long> rollingTotalLatencies = new ConcurrentLinkedQueue<>();
    private final AtomicInteger rollingTotalSize = new AtomicInteger(0);
    
    private final Queue<Long> rollingMatchLatencies = new ConcurrentLinkedQueue<>();
    private final AtomicInteger rollingMatchSize = new AtomicInteger(0);

    private final PersistenceService persistenceService;

    public TradingService() {
        this.persistenceService = null;
        
        // Initialize 4 independent matching partitions (one per CPU core configuration)
        int numPartitions = 4;
        this.partitions = new Partition[numPartitions];
        for (int i = 0; i < numPartitions; i++) {
            this.partitions[i] = new Partition(
                    i,
                    tradeHistory,
                    listeners,
                    rollingTotalLatencies,
                    rollingTotalSize,
                    rollingMatchLatencies,
                    rollingMatchSize,
                    null
            );
        }
    }

    @Autowired
    public TradingService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        
        // Initialize 4 independent matching partitions (one per CPU core configuration)
        int numPartitions = 4;
        this.partitions = new Partition[numPartitions];
        for (int i = 0; i < numPartitions; i++) {
            this.partitions[i] = new Partition(
                    i,
                    tradeHistory,
                    listeners,
                    rollingTotalLatencies,
                    rollingTotalSize,
                    rollingMatchLatencies,
                    rollingMatchSize,
                    persistenceService
            );
        }
    }

    public void registerListener(MarketDataListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(MarketDataListener listener) {
        listeners.remove(listener);
    }

    private Partition getPartition(String symbol) {
        int index = Math.abs(symbol.hashCode()) % partitions.length;
        return partitions[index];
    }

    /**
     * Submits an order asynchronously to the matching partition's Disruptor ring buffer.
     */
    public CompletableFuture<OrderResult> submitOrder(long traderId, String symbol, Side side, OrderType type, long price, long quantity) {
        long startNs = System.nanoTime();
        long orderId = orderIdGenerator.getAndIncrement();
        
        long orderPrice = (type == OrderType.MARKET) ? 
                (side == Side.BUY ? Long.MAX_VALUE : 0L) : price;
        
        Order order = new Order(orderId, traderId, symbol, side, type, orderPrice, quantity, startNs);
        CompletableFuture<OrderResult> future = new CompletableFuture<>();

        // Route to the designated partition
        Partition partition = getPartition(symbol);
        RingBuffer<OrderEvent> ringBuffer = partition.getRingBuffer();

        totalCount.increment();

        // Publish to RingBuffer (lock-free sequence claiming)
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setSubmit(order, future);
        } finally {
            ringBuffer.publish(sequence);
        }

        return future;
    }

    /**
     * Cancels an order asynchronously. Searches the partition lookup tables and publishes a cancel event.
     */
    public CompletableFuture<Boolean> cancelOrder(long orderId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Partition targetPartition = null;
        for (Partition p : partitions) {
            if (p.getOrderBook().getOrderLookup().containsKey(orderId)) {
                targetPartition = p;
                break;
            }
        }
        
        if (targetPartition == null) {
            future.complete(false);
            return future;
        }

        RingBuffer<OrderEvent> ringBuffer = targetPartition.getRingBuffer();
        
        totalCount.increment();

        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setCancel(orderId, future);
        } finally {
            ringBuffer.publish(sequence);
        }
        
        return future;
    }

    /**
     * Retrieve aggregated latency stats across all partitions.
     */
    public Map<String, Object> getLatencyMetrics() {
        long count = totalCount.sum();
        
        long sumLatency = 0, sumMatch = 0, sumQueue = 0;
        long minTotal = Long.MAX_VALUE, maxTotal = 0;
        long minMatch = Long.MAX_VALUE, maxMatch = 0;
        long minQueue = Long.MAX_VALUE, maxQueue = 0;

        for (Partition p : partitions) {
            sumLatency += p.getTotalLatencyNs();
            sumMatch += p.getTotalMatchNs();
            sumQueue += p.getTotalQueueNs();

            if (p.getTotalCount() > 0) {
                minTotal = Math.min(minTotal, p.getMinLatencyNs());
                maxTotal = Math.max(maxTotal, p.getMaxLatencyNs());
                minMatch = Math.min(minMatch, p.getMinMatchNs());
                maxMatch = Math.max(maxMatch, p.getMaxMatchNs());
                minQueue = Math.min(minQueue, p.getMinQueueNs());
                maxQueue = Math.max(maxQueue, p.getMaxQueueNs());
            }
        }

        long avgTotal = count == 0 ? 0 : sumLatency / count;
        long avgMatch = count == 0 ? 0 : sumMatch / count;
        long avgQueue = count == 0 ? 0 : sumQueue / count;

        if (minTotal == Long.MAX_VALUE) minTotal = 0;
        if (minMatch == Long.MAX_VALUE) minMatch = 0;
        if (minQueue == Long.MAX_VALUE) minQueue = 0;

        // Calculate percentiles from rolling buffers
        List<Long> totals = new ArrayList<>(rollingTotalLatencies);
        Collections.sort(totals);
        long p50Total = 0, p90Total = 0, p99Total = 0;
        if (!totals.isEmpty()) {
            int size = totals.size();
            p50Total = totals.get((int) (size * 0.50));
            p90Total = totals.get((int) (size * 0.90));
            p99Total = totals.get((int) (size * 0.99));
        }

        List<Long> matches = new ArrayList<>(rollingMatchLatencies);
        Collections.sort(matches);
        long p50Match = 0, p90Match = 0, p99Match = 0;
        if (!matches.isEmpty()) {
            int size = matches.size();
            p50Match = matches.get((int) (size * 0.50));
            p90Match = matches.get((int) (size * 0.90));
            p99Match = matches.get((int) (size * 0.99));
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalOrdersProcessed", count);
        
        metrics.put("minTotalLatencyNs", minTotal);
        metrics.put("maxTotalLatencyNs", maxTotal);
        metrics.put("avgTotalLatencyNs", avgTotal);
        metrics.put("p50TotalLatencyNs", p50Total);
        metrics.put("p90TotalLatencyNs", p90Total);
        metrics.put("p99TotalLatencyNs", p99Total);

        metrics.put("minMatchLatencyNs", minMatch);
        metrics.put("maxMatchLatencyNs", maxMatch);
        metrics.put("avgMatchLatencyNs", avgMatch);
        metrics.put("p50MatchLatencyNs", p50Match);
        metrics.put("p90MatchLatencyNs", p90Match);
        metrics.put("p99MatchLatencyNs", p99Match);

        metrics.put("minQueueWaitNs", minQueue);
        metrics.put("maxQueueWaitNs", maxQueue);
        metrics.put("avgQueueWaitNs", avgQueue);
        
        return metrics;
    }

    /**
     * Prints order book states across all partitions.
     */
    public void printOrderBook() {
        for (int i = 0; i < partitions.length; i++) {
            System.out.println("=========================================");
            System.out.println("   PARTITION " + i + " ORDER BOOK SNAPSHOT");
            System.out.println("=========================================");
            partitions[i].getOrderBook().printOrderBook();
        }
    }

    /**
     * Merges Bids and Asks from all partitions into an aggregated view.
     */
    public CompletableFuture<OrderBook> getOrderBookSnapshot() {
        return CompletableFuture.supplyAsync(() -> {
            OrderBook aggregated = new OrderBook();
            for (Partition p : partitions) {
                // Merge Bids
                for (var entry : p.getOrderBook().getBids().entrySet()) {
                    for (Order order : entry.getValue()) {
                        aggregated.addOrder(order);
                    }
                }
                // Merge Asks
                for (var entry : p.getOrderBook().getAsks().entrySet()) {
                    for (Order order : entry.getValue()) {
                        aggregated.addOrder(order);
                    }
                }
            }
            return aggregated;
        });
    }

    public List<Trade> getTradeHistory() {
        return new ArrayList<>(tradeHistory);
    }

    public void clear() {
        tradeHistory.clear();
        orderIdGenerator.set(1);
        totalCount.reset();
        
        rollingTotalLatencies.clear();
        rollingTotalSize.set(0);
        rollingMatchLatencies.clear();
        rollingMatchSize.set(0);
        
        for (Partition p : partitions) {
            p.clear();
        }
    }
    
    public void shutdown() {
        for (Partition p : partitions) {
            p.shutdown();
        }
        if (persistenceService != null) {
            persistenceService.shutdown();
        }
    }
}
