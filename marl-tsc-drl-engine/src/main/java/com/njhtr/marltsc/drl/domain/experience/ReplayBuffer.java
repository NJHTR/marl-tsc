package com.njhtr.marltsc.drl.domain.experience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ReplayBuffer {

    private final int capacity;
    private final List<Experience> buffer;
    private int position;

    public ReplayBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new ArrayList<>(capacity);
        this.position = 0;
    }

    public synchronized void add(Experience experience) {
        if (buffer.size() < capacity) {
            buffer.add(experience);
        } else {
            buffer.set(position, experience);
        }
        position = (position + 1) % capacity;
    }

    public synchronized List<Experience> sample(int batchSize) {
        int size = buffer.size();
        if (size == 0) return Collections.emptyList();
        int actualBatch = Math.min(batchSize, size);
        List<Experience> batch = new ArrayList<>(actualBatch);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < actualBatch; i++) {
            batch.add(buffer.get(random.nextInt(size)));
        }
        return batch;
    }

    public synchronized int size() {
        return buffer.size();
    }

    public synchronized void clear() {
        buffer.clear();
        position = 0;
    }
}
