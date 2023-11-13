package com.techcat.feline.datagen.queue;

import com.techcat.feline.datagen.model.Data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DataQueue {
    private static class QueueHolder {
        private static final BlockingQueue<Data> queue = new LinkedBlockingQueue<>();
    }

    private DataQueue() {
    }

    private static BlockingQueue<Data> getQueue() {
        return QueueHolder.queue;
    }

    public static boolean addData(Data data) {
        return getQueue().add(data);
    }

    public static Data getData() throws InterruptedException {
        return getQueue().take();
    }
}
