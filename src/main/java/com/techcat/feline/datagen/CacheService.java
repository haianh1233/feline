package com.techcat.feline.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class CacheService {
    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();

    public void addData(String topic, String keyData) {
        cache.computeIfAbsent(topic, k -> new ArrayList<>()).add(keyData);
    }

    public List<String> get(String topic) {
        return cache.get(topic);
    }

    public String getRandomKeyData(String topic) {
        long startTime = System.currentTimeMillis();
        long timeout = 10 * 1000; // 10 seconds in milliseconds

        while (System.currentTimeMillis() - startTime < timeout) {
            List<String> dataList = cache.get(topic);
            if (dataList != null && !dataList.isEmpty()) {
                int randomIndex = new Random().nextInt(dataList.size());
                return dataList.get(randomIndex);
            }

            // If data is not available, wait for a short period before trying again
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // If 10 seconds have passed and data is still not available, throw an exception
        throw new RuntimeException("Data not found after waiting for 10 seconds");
    }

}
