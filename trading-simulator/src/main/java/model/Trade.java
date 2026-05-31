package model;

public class Trade {

    private final long tradeId;
    private final long buyOrderId;
    private final long sellOrderId;
    private final long price;
    private final long quantity;
    private final long timestamp;
    private final String symbol;

    public Trade(
            long tradeId,
            long buyOrderId,
            long sellOrderId,
            long price,
            long quantity,
            long timestamp,
            String symbol
    ) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.symbol = symbol;
    }

    public long getTradeId() {
        return tradeId;
    }

    public long getBuyOrderId() {
        return buyOrderId;
    }

    public long getSellOrderId() {
        return sellOrderId;
    }

    public long getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return String.format("Trade[id=%d, buy=%d, sell=%d, price=%d, qty=%d, ts=%d, symbol=%s]",
                tradeId, buyOrderId, sellOrderId, price, quantity, timestamp, symbol);
    }
}
