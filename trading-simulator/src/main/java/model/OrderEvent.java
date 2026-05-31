package model;

import java.util.concurrent.CompletableFuture;

public class OrderEvent {
    public enum Type {
        SUBMIT,
        CANCEL
    }

    private Type type;
    private Order order;
    private long cancelOrderId;
    private CompletableFuture<OrderResult> submitFuture;
    private CompletableFuture<Boolean> cancelFuture;

    public Type getType() {
        return type;
    }

    public void setSubmit(Order order, CompletableFuture<OrderResult> future) {
        this.type = Type.SUBMIT;
        this.order = order;
        this.submitFuture = future;
        this.cancelOrderId = -1;
        this.cancelFuture = null;
    }

    public void setCancel(long cancelOrderId, CompletableFuture<Boolean> future) {
        this.type = Type.CANCEL;
        this.cancelOrderId = cancelOrderId;
        this.cancelFuture = future;
        this.order = null;
        this.submitFuture = null;
    }

    public Order getOrder() {
        return order;
    }

    public long getCancelOrderId() {
        return cancelOrderId;
    }

    public CompletableFuture<OrderResult> getSubmitFuture() {
        return submitFuture;
    }

    public CompletableFuture<Boolean> getCancelFuture() {
        return cancelFuture;
    }

    public void clear() {
        this.type = null;
        this.order = null;
        this.cancelOrderId = -1;
        this.submitFuture = null;
        this.cancelFuture = null;
    }
}
