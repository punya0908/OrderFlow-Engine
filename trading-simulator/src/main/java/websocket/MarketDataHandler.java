package websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import engine.OrderBook;
import model.Order;
import model.Trade;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import service.MarketDataListener;
import service.TradingService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MarketDataHandler extends TextWebSocketHandler implements MarketDataListener {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MarketDataHandler(TradingService tradingService) {
        tradingService.registerListener(this);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    @Override
    public void onTrades(List<Trade> trades) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("event", "trades");
        message.put("data", trades);
        broadcast(message);
    }

    @Override
    public void onBookUpdate(OrderBook orderBook) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("event", "book_update");

        List<Map<String, Object>> bidLevels = new ArrayList<>();
        for (Map.Entry<Long, Deque<Order>> entry : orderBook.getBids().entrySet()) {
            long totalQty = entry.getValue().stream().mapToLong(Order::getQuantity).sum();
            Map<String, Object> lvl = new LinkedHashMap<>();
            lvl.put("price", entry.getKey());
            lvl.put("quantity", totalQty);
            bidLevels.add(lvl);
        }

        List<Map<String, Object>> askLevels = new ArrayList<>();
        for (Map.Entry<Long, Deque<Order>> entry : orderBook.getAsks().entrySet()) {
            long totalQty = entry.getValue().stream().mapToLong(Order::getQuantity).sum();
            Map<String, Object> lvl = new LinkedHashMap<>();
            lvl.put("price", entry.getKey());
            lvl.put("quantity", totalQty);
            askLevels.add(lvl);
        }

        Map<String, Object> bookData = new LinkedHashMap<>();
        bookData.put("bids", bidLevels);
        bookData.put("asks", askLevels);

        message.put("data", bookData);
        broadcast(message);
    }

    private void broadcast(Object obj) {
        if (sessions.isEmpty()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(obj);
            TextMessage message = new TextMessage(payload);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        sessions.remove(session);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
