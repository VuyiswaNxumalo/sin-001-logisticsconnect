package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import io.javalin.Javalin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class IngestionServiceApp {

    record HubRecord(
            String hubId,
            String province,
            String sortingCenter,
            Boolean active,
            String notes
    ) {}

    public static void main(String[] args) throws Exception {
        List<HubRecord> hubs = loadAndCleanHubs();

        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/hubs", ctx -> ctx.json(hubs));
    }

    static List<HubRecord> loadAndCleanHubs() throws IOException {
        List<HubRecord> cleaned = new ArrayList<>();

        InputStream csvStream = IngestionServiceApp.class
                .getClassLoader()
                .getResourceAsStream("hubs-global.csv");

        if (csvStream == null) {
            throw new IOException("hubs-global.csv not found on classpath");
        }

        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(csvStream))
                .withSkipLines(1)
                .build()) {

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 4) continue;
                cleaned.add(cleanRow(row));
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse hubs-global.csv", e);
        }

        return cleaned;
    }

    private static HubRecord cleanRow(String[] row) {
        String hubId = normalizeSpacing(row[0]).toUpperCase();
        String province = titleCase(normalizeSpacing(row[1]));
        String sortingCenter = titleCase(normalizeSpacing(row[2]));
        String rawActive = normalizeSpacing(row[3]);

        // Boolean parsing added in the next commit - for now, pass through.
        return new HubRecord(hubId, province, sortingCenter, null, "active parsing: TODO");
    }

    private static String normalizeSpacing(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return value;
        String[] words = value.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0)))
              .append(w.substring(1))
              .append(" ");
        }
        return sb.toString().trim();
    }
}
