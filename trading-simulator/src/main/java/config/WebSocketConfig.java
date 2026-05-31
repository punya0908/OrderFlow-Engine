package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import service.TradingService;
import websocket.MarketDataHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TradingService tradingService;

    public WebSocketConfig(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketDataHandler(), "/ws/market-data")
                .setAllowedOrigins("*");
    }

    @Bean
    public MarketDataHandler marketDataHandler() {
        return new MarketDataHandler(tradingService);
    }
}
