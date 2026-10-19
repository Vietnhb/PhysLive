package com.example.backend.system.simulation.service;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Results of solving a signed plan with given slider values, kept so the same request is not solved twice (many
 * learners opening one assigned simulation, a slider moved back to a value it had). The solver is deterministic —
 * random draws are seeded — so a kept result is the result. Least recently used entries go first; the bound is the
 * total size of the kept results, set by physlive.simulation.compute-cache-max-bytes (0 keeps nothing).
 */
@Component
public class ComputeResultCache {
    private final long maxBytes;
    private final LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>(64, .75f, true);
    private long bytes;

    public ComputeResultCache(@Value("${physlive.simulation.compute-cache-max-bytes:67108864}") long maxBytes) {
        this.maxBytes = Math.max(0, maxBytes);
    }

    public synchronized byte[] get(String key) {
        return entries.get(key);
    }

    public synchronized void put(String key, byte[] value) {
        // one result may not take more than a quarter of the room, or a single huge run would empty the cache
        if (maxBytes == 0 || value.length > maxBytes / 4) return;
        byte[] previous = entries.put(key, value);
        bytes += value.length - (previous == null ? 0 : previous.length);
        for (Iterator<Map.Entry<String, byte[]>> eldest = entries.entrySet().iterator(); bytes > maxBytes && eldest.hasNext(); ) {
            bytes -= eldest.next().getValue().length;
            eldest.remove();
        }
    }
}
