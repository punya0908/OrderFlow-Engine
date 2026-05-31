package service;

import model.*;
import repository.OrderEntityRepository;
import repository.TradeEntityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class PersistenceService {

    private final OrderEntityRepository orderRepository;
    private final TradeEntityRepository tradeRepository;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService dbExecutor;
    private final boolean enabled;

    public PersistenceService(
            OrderEntityRepository orderRepository,
            TradeEntityRepository tradeRepository,
            PlatformTransactionManager transactionManager,
            @Value("${db.persistence.enabled:true}") boolean enabled
    ) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
        
        // Single-threaded worker to guarantee chronological ordering of updates
        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "db-persistence-worker");
            thread.setDaemon(true);
            return thread;
        });
        System.out.println("PersistenceService initialized. Database persistence enabled: " + enabled);
    }

    public void persistOrderPlacement(Order order) {
        if (!enabled) return;
        dbExecutor.submit(() -> {
            try {
                OrderEntity entity = new OrderEntity(order, "ACTIVE");
                orderRepository.save(entity);
            } catch (Exception e) {
                System.err.println("Database write error on order submit: " + e.getMessage());
            }
        });
    }

    public void persistOrderCancel(long orderId) {
        if (!enabled) return;
        dbExecutor.submit(() -> {
            try {
                orderRepository.findById(orderId).ifPresent(order -> {
                    order.setStatus("CANCELED");
                    orderRepository.save(order);
                });
            } catch (Exception e) {
                System.err.println("Database write error on order cancel: " + e.getMessage());
            }
        });
    }

    public void persistTrades(List<Trade> trades) {
        if (!enabled || trades.isEmpty()) return;
        dbExecutor.submit(() -> {
            try {
                transactionTemplate.execute(status -> {
                    for (Trade trade : trades) {
                        // Save Trade
                        tradeRepository.save(new TradeEntity(trade));

                        // Update Buy Order
                        orderRepository.findById(trade.getBuyOrderId()).ifPresent(buyOrder -> {
                            long rem = Math.max(0, buyOrder.getRemainingQuantity() - trade.getQuantity());
                            buyOrder.setRemainingQuantity(rem);
                            buyOrder.setStatus(rem == 0 ? "FILLED" : "PARTIALLY_FILLED");
                            orderRepository.save(buyOrder);
                        });

                        // Update Sell Order
                        orderRepository.findById(trade.getSellOrderId()).ifPresent(sellOrder -> {
                            long rem = Math.max(0, sellOrder.getRemainingQuantity() - trade.getQuantity());
                            sellOrder.setRemainingQuantity(rem);
                            sellOrder.setStatus(rem == 0 ? "FILLED" : "PARTIALLY_FILLED");
                            orderRepository.save(sellOrder);
                        });
                    }
                    return null;
                });
            } catch (Exception e) {
                System.err.println("Database write error on trades batch: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        dbExecutor.shutdown();
    }
}
