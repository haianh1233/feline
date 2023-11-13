package com.techcat.feline.datagen.producer;

import com.techcat.feline.datagen.DataInterpreter;
import com.techcat.feline.datagen.model.ConfigEntry;
import com.techcat.feline.datagen.model.Data;
import com.techcat.feline.datagen.queue.DataQueue;

public class DataGenProducer implements Runnable {
    private final DataInterpreter dataInterpreter;
    private final ConfigEntry config;

    public DataGenProducer(ConfigEntry config) {
        this.dataInterpreter = new DataInterpreter();
        this.config = config;
    }

    @Override
    public void run() {
        while (true) {
            generateData();
            applyThrottling();
        }
    }

    private void generateData() {
        Data data = dataInterpreter.interpretConfig(config);
        data.setTopic(config.getTopic());
        DataQueue.addData(data);
    }

    private void applyThrottling() {
        if (config.getConfig() != null && config.getConfig().getThrottle() != null) {
            try {
                Thread.sleep(config.getConfig().getThrottle().getMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
