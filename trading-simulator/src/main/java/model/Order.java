package model;

public class Order {

    private final long orderId;
    private final long traderId;
    private final String symbol;

    private final Side side;
    private final OrderType type;

    private long quantity;
    private final long price;

    private final long timestamp;

    public Order(
            long orderId,
            long traderId,
            String symbol,
            Side side,
            OrderType type,
            long price,
            long quantity,
            long timestamp
    ) {
        this.orderId = orderId;
        this.traderId = traderId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    public long getQuantity() {
        return quantity;
    }

    public void reduceQuantity(long qty) {
        this.quantity -= qty;
    }

    public long getPrice() {
        return price;
    }

    public Side getSide() {
        return side;
    }

    public long getOrderId() {
        return orderId;
    }

    public OrderType getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getTraderId() {
        return traderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("Order[id=%d, trader=%d, symbol=%s, side=%s, type=%s, price=%d, qty=%d, ts=%d]",
                orderId, traderId, symbol, side, type, price, quantity, timestamp);
    }
}
