package engine;

import model.Order;
import model.OrderType;
import model.Side;
import model.Trade;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.TreeMap;
import java.util.function.LongSupplier;

public class MatchingEngine {

    /**
     * Matches an incoming order against the OrderBook.
     * Generates a list of executed Trade records.
     *
     * @param incomingOrder The order being submitted.
     * @param orderBook The order book to match against and potentially place the order in.
     * @param tradeIdGenerator Supplier for generating unique trade IDs.
     * @return List of executed trades.
     */
    public List<Trade> match(Order incomingOrder, OrderBook orderBook, LongSupplier tradeIdGenerator) {
        List<Trade> trades = new ArrayList<>();
        
        if (incomingOrder.getQuantity() <= 0) {
            return trades;
        }

        Side side = incomingOrder.getSide();
        boolean isLimit = incomingOrder.getType() == OrderType.LIMIT;
        
        if (side == Side.BUY) {
            // Match BUY against asks (sell book)
            TreeMap<Long, Deque<Order>> asks = orderBook.getAsks();
            
            while (incomingOrder.getQuantity() > 0 && !asks.isEmpty()) {
                long bestAskPrice = asks.firstKey();
                
                // For limit orders, price must be >= best ask
                if (isLimit && incomingOrder.getPrice() < bestAskPrice) {
                    break;
                }
                
                Deque<Order> queue = asks.get(bestAskPrice);
                while (incomingOrder.getQuantity() > 0 && !queue.isEmpty()) {
                    Order restingSell = queue.peekFirst();
                    
                    long matchQty = Math.min(incomingOrder.getQuantity(), restingSell.getQuantity());
                    
                    // Reduce quantities
                    incomingOrder.reduceQuantity(matchQty);
                    restingSell.reduceQuantity(matchQty);
                    
                    // Generate Trade using the resting order's price
                    long tradeId = tradeIdGenerator.getAsLong();
                    trades.add(new Trade(
                            tradeId,
                            incomingOrder.getOrderId(),
                            restingSell.getOrderId(),
                            restingSell.getPrice(),
                            matchQty,
                            System.currentTimeMillis(),
                            incomingOrder.getSymbol()
                    ));
                    
                    // If resting order fully filled, remove it
                    if (restingSell.getQuantity() == 0) {
                        queue.pollFirst();
                        orderBook.getOrderLookup().remove(restingSell.getOrderId());
                    }
                }
                
                // If the price level queue is empty, remove the price level from the book
                if (queue.isEmpty()) {
                    asks.remove(bestAskPrice);
                }
            }
            
            // If the incoming order is LIMIT and has remaining quantity, add it to bids
            if (isLimit && incomingOrder.getQuantity() > 0) {
                orderBook.addOrder(incomingOrder);
            }
            
        } else {
            // Match SELL against bids (buy book)
            TreeMap<Long, Deque<Order>> bids = orderBook.getBids();
            
            while (incomingOrder.getQuantity() > 0 && !bids.isEmpty()) {
                long bestBidPrice = bids.firstKey();
                
                // For limit orders, price must be <= best bid
                if (isLimit && incomingOrder.getPrice() > bestBidPrice) {
                    break;
                }
                
                Deque<Order> queue = bids.get(bestBidPrice);
                while (incomingOrder.getQuantity() > 0 && !queue.isEmpty()) {
                    Order restingBuy = queue.peekFirst();
                    
                    long matchQty = Math.min(incomingOrder.getQuantity(), restingBuy.getQuantity());
                    
                    // Reduce quantities
                    incomingOrder.reduceQuantity(matchQty);
                    restingBuy.reduceQuantity(matchQty);
                    
                    // Generate Trade using the resting order's price
                    long tradeId = tradeIdGenerator.getAsLong();
                    trades.add(new Trade(
                            tradeId,
                            restingBuy.getOrderId(),
                            incomingOrder.getOrderId(),
                            restingBuy.getPrice(),
                            matchQty,
                            System.currentTimeMillis(),
                            incomingOrder.getSymbol()
                    ));
                    
                    // If resting order fully filled, remove it
                    if (restingBuy.getQuantity() == 0) {
                        queue.pollFirst();
                        orderBook.getOrderLookup().remove(restingBuy.getOrderId());
                    }
                }
                
                // If the price level queue is empty, remove the price level from the book
                if (queue.isEmpty()) {
                    bids.remove(bestBidPrice);
                }
            }
            
            // If the incoming order is LIMIT and has remaining quantity, add it to asks
            if (isLimit && incomingOrder.getQuantity() > 0) {
                orderBook.addOrder(incomingOrder);
            }
        }
        
        return trades;
    }
}
