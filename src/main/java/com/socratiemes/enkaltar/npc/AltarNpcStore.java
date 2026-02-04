package com.socratiemes.enkaltar.npc;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AltarNpcStore {
    private static final String INDEX_FILE = "npc_index.json";

    private final Path directory;
    private final Path indexPath;
    private final Gson gson;
    private final Map<Integer, AltarNpcRecord> records = new HashMap<>();
    private int nextId = 1;

    public AltarNpcStore(Path dataDirectory) {
        this.directory = dataDirectory.resolve("altar_npcs");
        this.indexPath = directory.resolve(INDEX_FILE);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        reload();
    }

    public synchronized void reload() {
        records.clear();
        int maxId = 0;
        readIndex();
        try {
            Files.createDirectories(directory);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "npc_*.json")) {
                for (Path path : stream) {
                    AltarNpcRecord record = readRecord(path);
                    if (record != null && record.id > 0) {
                        records.put(record.id, record);
                        if (record.id > maxId) {
                            maxId = record.id;
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Ignore to keep store usable.
        }

        if (nextId <= maxId) {
            nextId = maxId + 1;
        }
        writeIndex();
    }

    public synchronized List<AltarNpcRecord> getAll() {
        List<AltarNpcRecord> list = new ArrayList<>(records.values());
        list.sort(Comparator.comparingInt(r -> r.id));
        return list;
    }

    public synchronized AltarNpcRecord get(int id) {
        return records.get(id);
    }

    public synchronized AltarNpcRecord getByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (AltarNpcRecord record : records.values()) {
            if (record != null && uuid.equals(record.uuid)) {
                return record;
            }
        }
        return null;
    }

    public synchronized int allocateId() {
        int id = Math.max(1, nextId);
        while (records.containsKey(id) || Files.exists(recordPath(id))) {
            id++;
        }
        nextId = id + 1;
        writeIndex();
        return id;
    }

    public synchronized void save(AltarNpcRecord record) {
        if (record == null || record.id <= 0) {
            return;
        }
        records.put(record.id, record);
        writeRecord(record);
    }

    public synchronized void remove(int id) {
        records.remove(id);
        try {
            Files.deleteIfExists(recordPath(id));
        } catch (IOException e) {
            // Ignore delete errors.
        }
    }

    private Path recordPath(int id) {
        return directory.resolve("npc_" + id + ".json");
    }

    private AltarNpcRecord readRecord(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, AltarNpcRecord.class);
        } catch (IOException e) {
            return null;
        }
    }

    private void writeRecord(AltarNpcRecord record) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            return;
        }
        try (Writer writer = Files.newBufferedWriter(
                recordPath(record.id),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            gson.toJson(record, writer);
        } catch (IOException e) {
            // Ignore save errors.
        }
    }

    private void readIndex() {
        if (Files.notExists(indexPath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(indexPath, StandardCharsets.UTF_8)) {
            IndexData data = gson.fromJson(reader, IndexData.class);
            if (data != null && data.nextId > 0) {
                nextId = data.nextId;
            }
        } catch (IOException e) {
            // Ignore index read failures.
        }
    }

    private void writeIndex() {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            return;
        }
        IndexData data = new IndexData();
        data.nextId = nextId;
        try (Writer writer = Files.newBufferedWriter(
                indexPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            // Ignore index write failures.
        }
    }

    private static final class IndexData {
        int nextId;
    }
}
