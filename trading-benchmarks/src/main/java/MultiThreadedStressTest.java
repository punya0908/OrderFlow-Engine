import model.OrderGenerator;
import service.TradingService;

import java.util.concurrent.*;

public class MultiThreadedStressTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("   MULTI-THREADED ORDER SUBMISSION STRESS TEST   ");
        System.out.println("=================================================");

        int[] threadConfigs = {8, 16, 32, 64};
        int ordersPerTest = 1_000_000;

        for (int threads : threadConfigs) {
            runTest(threads, ordersPerTest);
        }
        System.out.println("All multi-threaded stress tests completed.");
    }

    private static void runTest(int numThreads, int totalOrders) throws Exception {
        System.out.printf("\n--- Running with %d Threads submitting %d orders ---\n", numThreads, totalOrders);

        TradingService service = new TradingService();
        OrderGenerator generator = new OrderGenerator();
        ExecutorService submitterPool = Executors.newFixedThreadPool(numThreads);

        // Pre-generate orders to isolate matching queue latency from generator CPU cycles
        System.out.println("Pre-generating orders...");
        model.Order[] orders = new model.Order[totalOrders];
        for (int i = 0; i < totalOrders; i++) {
            orders[i] = generator.generate(i);
        }

        System.out.println("Starting submission...");
        CountDownLatch latch = new CountDownLatch(totalOrders);

        long start = System.nanoTime();
        for (int i = 0; i < totalOrders; i++) {
            final model.Order o = orders[i];
            submitterPool.submit(() -> {
                service.submitOrder(o.getTraderId(), o.getSymbol(), o.getSide(), o.getType(), o.getPrice(), o.getQuantity())
                        .thenAccept(res -> latch.countDown());
            });
        }

        // Wait for all submissions to complete execution on the matching thread
        latch.await();
        long end = System.nanoTime();

        long durationNs = end - start;
        double durationSec = durationNs / 1_000_000_000.0;
        double throughput = totalOrders / durationSec;

        System.out.printf("Duration: %.3f seconds\n", durationSec);
        System.out.printf("Throughput: %.2f orders/sec\n", throughput);

        // Print latency metrics recorded inside the service
        var metrics = service.getLatencyMetrics();
        System.out.printf("Avg Total Latency:  %.2f us\n", ((Number) metrics.get("avgTotalLatencyNs")).doubleValue() / 1000.0);
        System.out.printf("P50 Total Latency:  %.2f us\n", ((Number) metrics.get("p50TotalLatencyNs")).doubleValue() / 1000.0);
        System.out.printf("P90 Total Latency:  %.2f us\n", ((Number) metrics.get("p90TotalLatencyNs")).doubleValue() / 1000.0);
        System.out.printf("P99 Total Latency:  %.2f us\n", ((Number) metrics.get("p99TotalLatencyNs")).doubleValue() / 1000.0);

        System.out.printf("Avg Match Latency:  %.2f us (Engine Only)\n", ((Number) metrics.get("avgMatchLatencyNs")).doubleValue() / 1000.0);
        System.out.printf("P99 Match Latency:  %.2f us (Engine Only)\n", ((Number) metrics.get("p99MatchLatencyNs")).doubleValue() / 1000.0);
        System.out.printf("Avg Queue Wait:     %.2f us (Queue Overhead)\n", ((Number) metrics.get("avgQueueWaitNs")).doubleValue() / 1000.0);

        // Clean up resources
        submitterPool.shutdown();
        submitterPool.awaitTermination(5, TimeUnit.SECONDS);
        service.clear();
        service.shutdown();
    }
}
