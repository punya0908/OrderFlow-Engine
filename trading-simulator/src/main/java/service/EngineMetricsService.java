package service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EngineMetricsService {

    private final TradingService tradingService;

    public EngineMetricsService(TradingService tradingService, MeterRegistry registry) {
        this.tradingService = tradingService;

        // Register custom Gauges mapping to TradingService metrics
        Gauge.builder("trading.orders.processed", this::getTotalOrdersProcessed)
                .description("Total number of orders processed since startup")
                .register(registry);

        Gauge.builder("trading.latency.total.avg", () -> getMetricValue("avgTotalLatencyNs"))
                .description("Average total roundtrip latency in nanoseconds")
                .register(registry);

        Gauge.builder("trading.latency.total.p50", () -> getMetricValue("p50TotalLatencyNs"))
                .description("P50 total roundtrip latency in nanoseconds")
                .register(registry);

        Gauge.builder("trading.latency.total.p90", () -> getMetricValue("p90TotalLatencyNs"))
                .description("P90 total roundtrip latency in nanoseconds")
                .register(registry);

        Gauge.builder("trading.latency.total.p99", () -> getMetricValue("p99TotalLatencyNs"))
                .description("P99 total roundtrip latency in nanoseconds")
                .register(registry);

        Gauge.builder("trading.latency.match.avg", () -> getMetricValue("avgMatchLatencyNs"))
                .description("Average match execution latency in nanoseconds")
                .register(registry);

        Gauge.builder("trading.latency.match.p50", () -> getMetricValue("p50MatchLatencyNs"))
                .description("P50 match execution latency in nanoseconds")
                .register(registry);

        Gauge.builder("trading.latency.match.p90", () -> getMetricValue("p90MatchLatencyNs"))
                .description("P90 match execution latency in nanoseconds")
                .register(registry);

        Gauge.builder("trading.latency.match.p99", () -> getMetricValue("p99MatchLatencyNs"))
                .description("P99 match execution latency in nanoseconds")
                .register(registry);

        Gauge.builder("trading.latency.queue.avg", () -> getMetricValue("avgQueueWaitNs"))
                .description("Average queue wait latency in nanoseconds")
                .register(registry);
    }

    private double getTotalOrdersProcessed() {
        Map<String, Object> metrics = tradingService.getLatencyMetrics();
        Object val = metrics.get("totalOrdersProcessed");
        return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
    }

    private double getMetricValue(String key) {
        Map<String, Object> metrics = tradingService.getLatencyMetrics();
        Object val = metrics.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
    }
}
