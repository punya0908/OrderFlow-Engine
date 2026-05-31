package model;

import java.util.Random;

public class OrderGenerator {

    private final Random random = new Random();
    private static final String[] SYMBOLS = {"AAPL", "MSFT", "TSLA", "NVDA"};

    /**
     * Generates a randomized limit order with partition-routable tickers.
     */
    public Order generate(long id) {
        Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
        String symbol = SYMBOLS[random.nextInt(SYMBOLS.length)];
        long price = 10000L + random.nextInt(1000); // Prices between 10000 and 10999
        long quantity = 1L + random.nextInt(100);    // Quantities between 1 and 100

        return new Order(
                id,
                1000L + random.nextInt(100), // traderId
                symbol,
                side,
                OrderType.LIMIT,
                price,
                quantity,
                System.nanoTime()
        );
    }
}
