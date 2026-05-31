import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"config", "controller", "websocket", "service", "repository"})
public class TradingApplication {

    public static void main(String[] args) {
        loadEnv();
        SpringApplication.run(TradingApplication.class, args);
    }

    public static void loadEnv() {
        java.io.File envFile = new java.io.File(".env");
        if (!envFile.exists()) {
            envFile = new java.io.File("../.env");
        }
        if (envFile.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        if (val.startsWith("\"") && val.endsWith("\"")) {
                            val = val.substring(1, val.length() - 1);
                        }
                        System.setProperty(key, val);
                    }
                }
                System.out.println("Environment variables loaded from .env file successfully.");
            } catch (java.io.IOException e) {
                System.err.println("Failed to load .env file: " + e.getMessage());
            }
        } else {
            System.out.println("Warning: .env file not found. Falling back to default properties/environment variables.");
        }
    }
}
