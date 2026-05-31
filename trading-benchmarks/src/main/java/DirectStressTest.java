import engine.MatchingEngine;
import engine.OrderBook;
import model.Order;
import model.OrderGenerator;

import java.util.concurrent.atomic.AtomicLong;

public class DirectStressTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   DIRECT MATCHING ENGINE STRESS TEST (10M ORDERS)");
        System.out.println("=================================================");

        OrderBook orderBook = new OrderBook();
        MatchingEngine matchingEngine = new MatchingEngine();
        OrderGenerator generator = new OrderGenerator();
        AtomicLong tradeIdGen = new AtomicLong(1);

        int totalOrders = 10_000_000;
        long[] latencies = new long[totalOrders];

        System.out.println("Warming up engine (1,000,000 orders)...");
        for (int i = 0; i < 1_000_000; i++) {
            Order order = generator.generate(i);
            matchingEngine.match(order, orderBook, tradeIdGen::getAndIncrement);
        }

        // Reset book for actual run to ensure clean starting state
        orderBook.getBids().clear();
        orderBook.getAsks().clear();
        orderBook.getOrderLookup().clear();
        tradeIdGen.set(1);

        System.out.println("Running stress test (10,000,000 orders)...");
        long startTest = System.nanoTime();
        for (int i = 0; i < totalOrders; i++) {
            Order order = generator.generate(i);
            
            long startOrder = System.nanoTime();
            matchingEngine.match(order, orderBook, tradeIdGen::getAndIncrement);
            long endOrder = System.nanoTime();
            
            latencies[i] = endOrder - startOrder;
        }
        long endTest = System.nanoTime();

        long durationNs = endTest - startTest;
        double durationSec = durationNs / 1_000_000_000.0;
        double throughput = totalOrders / durationSec;

        System.out.println("\n--- Performance Metrics ---");
        System.out.printf("Total Duration: %.3f seconds\n", durationSec);
        System.out.printf("Throughput: %.2f orders/sec\n", throughput);

        System.out.println("Sorting latencies to compute percentiles (please wait)...");
        java.util.Arrays.sort(latencies);

        long min = latencies[0];
        long max = latencies[totalOrders - 1];
        long sum = 0;
        for (long l : latencies) sum += l;
        double avg = (double) sum / totalOrders;

        long p50 = latencies[(int) (totalOrders * 0.50)];
        long p90 = latencies[(int) (totalOrders * 0.90)];
        long p95 = latencies[(int) (totalOrders * 0.95)];
        long p99 = latencies[(int) (totalOrders * 0.99)];
        long p999 = latencies[(int) (totalOrders * 0.999)];

        System.out.printf("Min Latency: %d ns\n", min);
        System.out.printf("Max Latency: %d ns (%.3f ms)\n", max, max / 1_000_000.0);
        System.out.printf("Avg Latency: %.2f ns (%.3f us)\n", avg, avg / 1000.0);
        System.out.printf("P50 Latency: %d ns\n", p50);
        System.out.printf("P90 Latency: %d ns\n", p90);
        System.out.printf("P95 Latency: %d ns\n", p95);
        System.out.printf("P99 Latency: %d ns (%.3f us)\n", p99, p99 / 1000.0);
        System.out.printf("P99.9 Latency: %d ns (%.3f us)\n", p999, p999 / 1000.0);
        System.out.println("=================================================");
    }
}
