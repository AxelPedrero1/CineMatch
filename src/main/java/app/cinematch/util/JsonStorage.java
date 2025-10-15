package app.cinematch.util;

import app.cinematch.model.HistoryEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class JsonStorage {
    private static final File FILE = new File("src/main/resources/storage.json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static synchronized void addOrUpdate(String title, String status) {
        List<HistoryEntry> all = loadAll();
        all.removeIf(e -> e.title().equalsIgnoreCase(title));
        all.add(new HistoryEntry(title, status, LocalDateTime.now().toString()));
        saveAll(all);
    }

    public static synchronized List<HistoryEntry> loadAll() {
        if (!FILE.exists()) return new ArrayList<>();
        try {
            return MAPPER.readValue(FILE, new TypeReference<List<HistoryEntry>>(){});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static synchronized List<String> getByStatus(String status) {
        return loadAll().stream()
                .filter(e -> e.status().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(HistoryEntry::dateTimeIso).reversed())
                .map(HistoryEntry::title)
                .collect(Collectors.toList());
    }

    public static synchronized void saveAll(List<HistoryEntry> all) {
        try {
            FILE.getParentFile().mkdirs();
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(FILE, all);
        } catch (IOException ignored) {}
    }
}
