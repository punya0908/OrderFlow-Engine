package benchmark;

import engine.MatchingEngine;
import engine.OrderBook;
import model.Order;
import model.OrderGenerator;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(0)
public class MatchingEngineBenchmark {

    private OrderBook orderBook;
    private MatchingEngine matchingEngine;
    private OrderGenerator generator;
    private AtomicLong tradeIdGenerator;

    @Setup(Level.Trial)
    public void setUp() {
        orderBook = new OrderBook();
        matchingEngine = new MatchingEngine();
        generator = new OrderGenerator();
        tradeIdGenerator = new AtomicLong(1);
    }

    @State(Scope.Thread)
    public static class ThreadState {
        long count = 0;
    }

    @Benchmark
    public void testRawMatching(ThreadState state) {
        Order order = generator.generate(state.count++);
        matchingEngine.match(order, orderBook, tradeIdGenerator::getAndIncrement);
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.runner.options.Options opt = new org.openjdk.jmh.runner.options.OptionsBuilder()
                .include(MatchingEngineBenchmark.class.getSimpleName())
                .forks(0)
                .build();
        new org.openjdk.jmh.runner.Runner(opt).run();
    }
}
