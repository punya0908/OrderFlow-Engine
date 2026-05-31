package model;

import jakarta.persistence.*;

@Entity
@Table(name = "trades")
public class TradeEntity {

    @Id
    @Column(name = "trade_id")
    private Long tradeId;

    @Column(name = "buy_order_id")
    private Long buyOrderId;

    @Column(name = "sell_order_id")
    private Long sellOrderId;

    @Column(name = "price")
    private Long price;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "timestamp")
    private Long timestamp;

    @Column(name = "symbol")
    private String symbol;

    public TradeEntity() {}

    public TradeEntity(Trade trade) {
        this.tradeId = trade.getTradeId();
        this.buyOrderId = trade.getBuyOrderId();
        this.sellOrderId = trade.getSellOrderId();
        this.price = trade.getPrice();
        this.quantity = trade.getQuantity();
        this.timestamp = trade.getTimestamp();
        this.symbol = trade.getSymbol();
    }

    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

    public Long getBuyOrderId() {
        return buyOrderId;
    }

    public void setBuyOrderId(Long buyOrderId) {
        this.buyOrderId = buyOrderId;
    }

    public Long getSellOrderId() {
        return sellOrderId;
    }

    public void setSellOrderId(Long sellOrderId) {
        this.sellOrderId = sellOrderId;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
