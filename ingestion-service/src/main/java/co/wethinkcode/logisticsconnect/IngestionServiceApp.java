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

    private static final Set<String> PLACEHOLDER_VALUES = Set.of(
            "n/a", "na", "tbd", "unknown", "-", "nan", ""
    );

    private static final Set<String> TRUE_VALUES = Set.of("y", "yes", "true", "1");
    private static final Set<String> FALSE_VALUES = Set.of("n", "no", "false", "0");

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

        BooleanResult active = parseBoolean(rawActive);

        return new HubRecord(hubId, province, sortingCenter, active.value, active.note);
    }

    private record BooleanResult(Boolean value, String note) {}

    private static BooleanResult parseBoolean(String raw) {
        String normalized = raw.trim().toLowerCase();

        if (PLACEHOLDER_VALUES.contains(normalized)) {
            return new BooleanResult(null,
                    "active was missing/placeholder ('" + raw + "') - flagged for follow-up");
        }
        if (TRUE_VALUES.contains(normalized)) {
            return new BooleanResult(true, null);
        }
        if (FALSE_VALUES.contains(normalized)) {
            return new BooleanResult(false, null);
        }

        return new BooleanResult(null,
                "active had an unrecognized value (\'" + raw + "\') - flagged for follow-up");
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
