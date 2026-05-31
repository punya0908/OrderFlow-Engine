package model;

import java.util.List;

public class OrderResult {
    private final Order order;
    private final List<Trade> trades;
    private final long latencyNs;

    public OrderResult(Order order, List<Trade> trades, long latencyNs) {
        this.order = order;
        this.trades = trades;
        this.latencyNs = latencyNs;
    }

    public Order getOrder() {
        return order;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public long getLatencyNs() {
        return latencyNs;
    }
}
