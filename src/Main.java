
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    static class Sale {
        String id, date, drink, barista, payment, note;
        int volume;
        double price;

        Sale(String[] row) {
            id = row[0];
            date = row[1];
            drink = row[2];
            volume = Integer.parseInt(row[3]);
            price = Double.parseDouble(row[4].replace(",", "."));
            barista = row[5];
            payment = row[6];
            note = row[7];
        }

        boolean invalid() {
            return payment.isBlank() || price == 0.0;
        }
    }

    static String[] parseCSV(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }

        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    static List<Sale> readSeptember(Path file) throws IOException {
        List<Sale> sales = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] row = parseCSV(line);
                    if (row.length == 8) {
                        sales.add(new Sale(row));
                    }
                } catch (Exception e) {
                    System.out.println("Skipping invalid September row: " + line);
                }
            }
        }

        return sales;
    }

    static List<Sale> readOctober(Path file) throws IOException {
        Charset charset = Charset.forName("windows-1251");

        List<Sale> sales = new ArrayList<>();

        List<String> lines = Files.readAllLines(file, charset);

        for (int i = 1; i < lines.size(); i++) {
            try {
                String[] row = parseCSV(lines.get(i));
                if (row.length == 8) {
                    sales.add(new Sale(row));
                }
            } catch (Exception e) {
                System.out.println("Skipping invalid October row: " + lines.get(i));
            }
        }

        return sales;
    }

    static long countSeptemberCards(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.skip(1)
                    .map(Main::parseCSV)
                    .filter(row -> row.length == 8)
                    .filter(row -> row[6].trim().equalsIgnoreCase("карта"))
                    .count();
        }
    }

    public static void main(String[] args) throws IOException {
        Path septemberFile = Paths.get("data/sales_september.csv");
        Path octoberFile = Paths.get("data/sales_october.csv");

        List<Sale> september = readSeptember(septemberFile);
        List<Sale> october = readOctober(octoberFile);

        List<Sale> allSales = new ArrayList<>();
        allSales.addAll(september);
        allSales.addAll(october);

        long totalRows = allSales.size();

        long emptyPayments = allSales.stream()
                .filter(s -> s.payment.isBlank()).count();

        long zeroPrices = allSales.stream()
                .filter(s -> s.price == 0.0).count();

        List<Sale> validSales = allSales.stream()
                .filter(s -> !s.invalid())
                .collect(Collectors.toList());

        Map<String, Double> revenueByDrink = validSales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.drink,
                        Collectors.summingDouble(s -> s.price)));

        String bestDrink = revenueByDrink.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        long bestDrinkRevenue = Math.round(
                revenueByDrink.getOrDefault(bestDrink, 0.0));

        String busiestBarista = october.stream()
                .collect(Collectors.groupingBy(
                        s -> s.barista,
                        Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        long septemberCards = countSeptemberCards(septemberFile);

        String bestDate = validSales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.date,
                        Collectors.summingDouble(s -> s.price)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        List<String> report = Arrays.asList(
                "1. Total sales rows: " + totalRows,
                "2. Empty payment rows: " + emptyPayments,
                "3. Zero-price rows: " + zeroPrices,
                "4. Best drink by revenue: " + bestDrink,
                "5. Revenue of best drink: " + bestDrinkRevenue + " rubles",
                "6. Busiest October barista: " + busiestBarista,
                "7. September card payments: " + septemberCards,
                "8. Best sales date: " + bestDate
        );

        Files.write(Paths.get("report.txt"), report, StandardCharsets.UTF_8);

        report.forEach(System.out::println);
        System.out.println("\nReport saved to report.txt");
    }
}
