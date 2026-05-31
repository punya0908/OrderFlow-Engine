package model;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "trader_id")
    private Long traderId;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "side")
    private String side;

    @Column(name = "type")
    private String type;

    @Column(name = "price")
    private Long price;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "remaining_quantity")
    private Long remainingQuantity;

    @Column(name = "status")
    private String status;

    @Column(name = "timestamp")
    private Long timestamp;

    public OrderEntity() {}

    public OrderEntity(Order order, String status) {
        this.orderId = order.getOrderId();
        this.traderId = order.getTraderId();
        this.symbol = order.getSymbol();
        this.side = order.getSide().name();
        this.type = order.getType().name();
        this.price = order.getPrice();
        this.quantity = order.getQuantity();
        this.remainingQuantity = order.getQuantity();
        this.status = status;
        this.timestamp = order.getTimestamp();
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getTraderId() {
        return traderId;
    }

    public void setTraderId(Long traderId) {
        this.traderId = traderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Long getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(Long remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
