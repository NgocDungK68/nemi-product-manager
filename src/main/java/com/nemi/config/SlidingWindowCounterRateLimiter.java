package com.nemi.config;

import java.util.TreeMap;

public class SlidingWindowCounterRateLimiter {
    private final TreeMap<Long, Integer> windowSegments = new TreeMap<>();
    private final int limit;
    private final long windowSize;
    private final long segmentSize;

    public SlidingWindowCounterRateLimiter(int limit, long windowSize, long segmentSize) {
        this.limit = limit;
        this.windowSize = windowSize;
        this.segmentSize = segmentSize;
    }

    public synchronized boolean allowRequest() {
        long currentSegment = System.currentTimeMillis() / segmentSize;

        // Remove outdated segments
        windowSegments.entrySet().removeIf(entry -> entry.getKey() < currentSegment - (windowSize / segmentSize));

        // Sum the counts from active segments
        int currentCount = windowSegments.values().stream().mapToInt(Integer::intValue).sum();

        if (currentCount < limit) {
            windowSegments.put(currentSegment, windowSegments.getOrDefault(currentSegment, 0) + 1);
            return true;
        }
        return false;
    }
}