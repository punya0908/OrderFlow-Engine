package service;

import model.Trade;
import engine.OrderBook;
import java.util.List;

public interface MarketDataListener {
    /**
     * Triggered when trades are executed.
     */
    void onTrades(List<Trade> trades);

    /**
     * Triggered when the order book changes.
     */
    void onBookUpdate(OrderBook orderBook);
}
