package controller;

import model.OrderResult;
import model.Trade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.TradingService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TradingController {

    private final TradingService tradingService;

    public TradingController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping("/orders")
    public CompletableFuture<ResponseEntity<OrderResult>> submitOrder(@RequestBody OrderRequest request) {
        return tradingService.submitOrder(
                request.getTraderId(),
                request.getSymbol() != null ? request.getSymbol() : "BTC-USD",
                request.getSide(),
                request.getType(),
                request.getPrice(),
                request.getQuantity()
        ).thenApply(ResponseEntity::ok);
    }

    @DeleteMapping("/orders/{id}")
    public CompletableFuture<ResponseEntity<Boolean>> cancelOrder(@PathVariable("id") long orderId) {
        return tradingService.cancelOrder(orderId)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/book")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getOrderBook() {
        return tradingService.getOrderBookSnapshot().thenApply(book -> {
            List<Map<String, Object>> bidLevels = new java.util.ArrayList<>();
            for (var entry : book.getBids().entrySet()) {
                long totalQty = entry.getValue().stream().mapToLong(o -> o.getQuantity()).sum();
                Map<String, Object> lvl = new java.util.LinkedHashMap<>();
                lvl.put("price", entry.getKey());
                lvl.put("quantity", totalQty);
                bidLevels.add(lvl);
            }

            List<Map<String, Object>> askLevels = new java.util.ArrayList<>();
            for (var entry : book.getAsks().entrySet()) {
                long totalQty = entry.getValue().stream().mapToLong(o -> o.getQuantity()).sum();
                Map<String, Object> lvl = new java.util.LinkedHashMap<>();
                lvl.put("price", entry.getKey());
                lvl.put("quantity", totalQty);
                askLevels.add(lvl);
            }

            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("bids", bidLevels);
            response.put("asks", askLevels);
            return ResponseEntity.ok(response);
        });
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(tradingService.getLatencyMetrics());
    }

    @GetMapping("/trades")
    public ResponseEntity<List<Trade>> getTrades() {
        return ResponseEntity.ok(tradingService.getTradeHistory());
    }

    @PostMapping("/clear")
    public ResponseEntity<String> clear() {
        tradingService.clear();
        return ResponseEntity.ok("Cleared");
    }
}
