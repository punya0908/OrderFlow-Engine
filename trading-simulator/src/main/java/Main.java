import model.*;
import service.TradingService;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        TradingService tradingService = new TradingService();

        System.out.println("======================================================================");
        System.out.println("   LOW-LATENCY STOCK EXCHANGE MATCHING ENGINE SIMULATOR DEMO          ");
        System.out.println("======================================================================");

        // 1. Initial State
        System.out.println("\n--- Step 1: Initial Order Book state (should be empty) ---");
        tradingService.printOrderBook();

        // 2. Scenario from USER_REQUEST:
        // SELL 100 @ 101
        // SELL 50 @ 101
        // BUY 120 @ 102
        System.out.println("--- Step 2: Executing prompt example scenario for AAPL ---");
        
        System.out.println("Submitting: SELL 100 @ 101 (Limit Order)...");
        List<Trade> trades1 = tradingService.submitOrder(101L, "AAPL", Side.SELL, OrderType.LIMIT, 101L, 100L).join().getTrades();
        printTrades(trades1);

        System.out.println("Submitting: SELL 50 @ 101 (Limit Order)...");
        List<Trade> trades2 = tradingService.submitOrder(102L, "AAPL", Side.SELL, OrderType.LIMIT, 101L, 50L).join().getTrades();
        printTrades(trades2);

        // Print book after asks are added
        System.out.println("\nOrder Book after SELL orders posted:");
        tradingService.printOrderBook();

        System.out.println("Submitting: BUY 120 @ 102 (Limit Order)...");
        List<Trade> trades3 = tradingService.submitOrder(201L, "AAPL", Side.BUY, OrderType.LIMIT, 102L, 120L).join().getTrades();
        printTrades(trades3);

        // Print final book for this scenario
        System.out.println("\nOrder Book after BUY match execution:");
        tradingService.printOrderBook();

        // Validate prompt expectations:
        // Expected: Trade 100 @ 101, Trade 20 @ 101. Remaining: SELL 30 @ 101.
        System.out.println("Checking scenario results...");
        boolean matchSuccessful = trades3.size() == 2 &&
                trades3.get(0).getQuantity() == 100L && trades3.get(0).getPrice() == 101L &&
                trades3.get(1).getQuantity() == 20L && trades3.get(1).getPrice() == 101L;

        if (matchSuccessful) {
            System.out.println(">> SUCCESS: Scenario matched expectations exactly! [Trade 100 @ 101, Trade 20 @ 101]");
        } else {
            System.err.println(">> FAILURE: Scenario did not match expectations.");
        }

        // 3. Demonstrating Market Orders
        System.out.println("\n--- Step 3: Demonstrating Market Orders ---");
        System.out.println("Submitting: BUY 50 (Market Order)...");
        List<Trade> marketTrades = tradingService.submitOrder(202L, "AAPL", Side.BUY, OrderType.MARKET, 0L, 50L).join().getTrades();
        printTrades(marketTrades);

        System.out.println("\nOrder Book after Market Order matching:");
        tradingService.printOrderBook();

        // 4. Demonstrating Order Cancellation
        System.out.println("--- Step 4: Demonstrating Order Cancellation ---");
        System.out.println("Submitting a Buy Limit Order: BUY 100 @ 95...");
        List<Trade> limitBuy = tradingService.submitOrder(203L, "AAPL", Side.BUY, OrderType.LIMIT, 95L, 100L).join().getTrades();
        printTrades(limitBuy);

        System.out.println("Current Order Book:");
        tradingService.printOrderBook();

        // Look up the order ID: it should be ID 5
        long orderToCancel = 5L;
        System.out.printf("Attempting to cancel Order ID %d...\n", orderToCancel);
        boolean canceled = tradingService.cancelOrder(orderToCancel).join();
        System.out.printf("Cancel result: %b\n", canceled);

        System.out.println("\nOrder Book after cancellation:");
        tradingService.printOrderBook();
        
        System.out.println("======================================================================");
        System.out.println("   DEMO COMPLETED SUCCESSFULLY                                       ");
        System.out.println("======================================================================");

        // 5. Interactive CLI loop
        System.out.println("\n======================================================================");
        System.out.println("   INTERACTIVE CLI MODE STARTED                                       ");
        System.out.println("   Type 'help' to see the list of available commands.                 ");
        System.out.println("======================================================================");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\nMatchingEngineCLI> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                System.out.println("Exiting Interactive CLI. Goodbye!");
                break;
            }
            
            try {
                handleCommand(line, tradingService);
            } catch (Exception e) {
                System.err.println("Error executing command: " + e.getMessage());
            }
        }
        
        scanner.close();
        
        // Shutdown executor service
        tradingService.shutdown();
    }

    private static void handleCommand(String line, TradingService tradingService) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        
        switch (command) {
            case "help":
                printHelp();
                break;
            case "book":
                tradingService.printOrderBook();
                break;
            case "metrics":
                System.out.println("\nEngine Latency Metrics:");
                var metrics = tradingService.getLatencyMetrics();
                metrics.forEach((k, v) -> System.out.println("  " + k + ": " + v));
                break;
            case "clear":
                tradingService.clear();
                System.out.println("Order book and history cleared successfully.");
                break;
            case "cancel":
                if (parts.length < 2) {
                    System.err.println("Usage: cancel <orderId>");
                    return;
                }
                long orderId = Long.parseLong(parts[1]);
                System.out.printf("Attempting to cancel Order ID %d...\n", orderId);
                boolean canceled = tradingService.cancelOrder(orderId).join();
                System.out.printf("Cancel result: %b\n", canceled);
                break;
            case "buy":
            case "sell":
                if (parts.length < 4) {
                    System.err.println("Usage: <buy|sell> <symbol> <quantity> <price|market>");
                    return;
                }
                String symbol = parts[1].toUpperCase();
                long quantity = Long.parseLong(parts[2]);
                Side side = command.equals("buy") ? Side.BUY : Side.SELL;
                
                String priceOrType = parts[3].toLowerCase();
                OrderType orderType = priceOrType.equals("market") ? OrderType.MARKET : OrderType.LIMIT;
                long price = 0;
                if (orderType == OrderType.LIMIT) {
                    price = Long.parseLong(priceOrType);
                }
                
                // Using a default traderId = 1000
                long traderId = 1000L;
                
                System.out.printf("Submitting order: %s %d %s %s...\n", 
                        side, quantity, symbol, orderType == OrderType.LIMIT ? ("@ " + price) : "MARKET");
                
                List<Trade> trades = tradingService.submitOrder(traderId, symbol, side, orderType, price, quantity)
                        .join()
                        .getTrades();
                printTrades(trades);
                break;
            default:
                System.err.println("Unknown command. Type 'help' to see the list of available commands.");
        }
    }

    private static void printHelp() {
        System.out.println("\nAvailable CLI Commands:");
        System.out.println("  buy <symbol> <quantity> <price>    Submit a Limit BUY order");
        System.out.println("  buy <symbol> <quantity> market     Submit a Market BUY order");
        System.out.println("  sell <symbol> <quantity> <price>   Submit a Limit SELL order");
        System.out.println("  sell <symbol> <quantity> market    Submit a Market SELL order");
        System.out.println("  cancel <orderId>                   Cancel an active order");
        System.out.println("  book                               Print a snapshot of all order books");
        System.out.println("  metrics                            Print latency and processing metrics");
        System.out.println("  clear                              Clear all books and transaction history");
        System.out.println("  help                               Show this help menu");
        System.out.println("  exit / quit                        Exit the application");
    }

    private static void printTrades(List<Trade> trades) {
        if (trades.isEmpty()) {
            System.out.println("  [No trades executed]");
        } else {
            System.out.println("  Executed trades:");
            for (Trade t : trades) {
                System.out.printf("    - Trade ID: %d | Buy Order ID: %d | Sell Order ID: %d | Price: %d | Qty: %d\n",
                        t.getTradeId(), t.getBuyOrderId(), t.getSellOrderId(), t.getPrice(), t.getQuantity());
            }
        }
    }
}