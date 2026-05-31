package engine;

import model.Order;
import model.Side;

import java.util.*;

public class OrderBook {

    private final TreeMap<Long, Deque<Order>> bids =
            new TreeMap<>(Comparator.reverseOrder());

    private final TreeMap<Long, Deque<Order>> asks =
            new TreeMap<>();

    // Maintain HashMap for O(1) lookup/cancel
    private final Map<Long, Order> orderLookup = new HashMap<>();

    public TreeMap<Long, Deque<Order>> getBids() {
        return bids;
    }

    public TreeMap<Long, Deque<Order>> getAsks() {
        return asks;
    }

    public Map<Long, Order> getOrderLookup() {
        return orderLookup;
    }

    public void addOrder(Order order) {
        TreeMap<Long, Deque<Order>> book =
                order.getSide() == Side.BUY
                        ? bids
                        : asks;

        book.computeIfAbsent(
                order.getPrice(),
                p -> new ArrayDeque<>()
        ).addLast(order);

        orderLookup.put(order.getOrderId(), order);
    }

    public boolean cancelOrder(long orderId) {
        Order order = orderLookup.remove(orderId);
        if (order == null) {
            return false;
        }

        TreeMap<Long, Deque<Order>> book =
                order.getSide() == Side.BUY
                        ? bids
                        : asks;

        Deque<Order> queue = book.get(order.getPrice());
        if (queue != null) {
            // Queue removal in ArrayDeque is O(N) where N is queue size at this price level
            queue.remove(order);
            if (queue.isEmpty()) {
                book.remove(order.getPrice());
            }
        }
        return true;
    }

    public Order getOrder(long orderId) {
        return orderLookup.get(orderId);
    }

    /**
     * Prints the current state of the order book in a clean, human-readable format.
     */
    public void printOrderBook() {
        System.out.println("========================================");
        System.out.println("              ORDER BOOK                ");
        System.out.println("========================================");
        
        // Print ASKS (Sells) - highest price at the top, lowest at the bottom
        System.out.println("--- ASKS (SELL SIDE) ---");
        List<Map.Entry<Long, Deque<Order>>> askEntries = new ArrayList<>(asks.entrySet());
        // Print asks from highest price to lowest price for standard visual depth
        for (int i = askEntries.size() - 1; i >= 0; i--) {
            Map.Entry<Long, Deque<Order>> entry = askEntries.get(i);
            long price = entry.getKey();
            long totalQty = entry.getValue().stream().mapToLong(Order::getQuantity).sum();
            System.out.printf("  Price: %8d | Total Qty: %6d | Orders: %d\n", price, totalQty, entry.getValue().size());
        }
        if (asks.isEmpty()) {
            System.out.println("  [Empty]");
        }

        System.out.println("----------------------------------------");
        
        // Print BIDS (Buys) - highest price at the top, lowest at the bottom
        System.out.println("--- BIDS (BUY SIDE) ---");
        for (Map.Entry<Long, Deque<Order>> entry : bids.entrySet()) {
            long price = entry.getKey();
            long totalQty = entry.getValue().stream().mapToLong(Order::getQuantity).sum();
            System.out.printf("  Price: %8d | Total Qty: %6d | Orders: %d\n", price, totalQty, entry.getValue().size());
        }
        if (bids.isEmpty()) {
            System.out.println("  [Empty]");
        }
        System.out.println("========================================\n");
    }
}
