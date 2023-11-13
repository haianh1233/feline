package com.techcat.feline.datagen;

import com.techcat.feline.datagen.model.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class TopicCacheManager {
    private static class Holder {
        private static final Map<String, List<String>> cache = new ConcurrentHashMap<>();
        private static final int TIME_OUT = 10 * 1000; // 10 seconds in milliseconds
        private static final int DELAY = 100; // 100 milliseconds
    }

    private TopicCacheManager() {}

    private static Map<String, List<String>> getCache() {
        return Holder.cache;
    }

    public static void addData(String topic, Data data) {
        getCache().computeIfAbsent(topic, k -> new ArrayList<>()).add((String) data.getKey());
    }

    public static String getRandomKeyData(String topic) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < Holder.TIME_OUT) {
            List<String> dataList = getCache().get(topic);
            if (dataList != null && !dataList.isEmpty()) {
                int randomIndex = new Random().nextInt(dataList.size());
                return dataList.get(randomIndex);
            }

            // If data is not available, wait for a short period before trying again
            try {
                Thread.sleep(Holder.DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // If 10 seconds have passed and data is still not available, throw an exception
        throw new RuntimeException("Data not found after waiting for 10 seconds");
    }

}
