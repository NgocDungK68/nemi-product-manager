package com.nemi.filter;

import java.util.TreeMap;

public class SlidingWindowCounterRateLimiter {
    // demo tam, day moi la limit tren toan bo api chu chua limit theo ip
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

// redis + bucket4j


//@Component
//public class RedisRateLimitingFilter implements Filter {
//
//    private final ProxyManager<String> proxyManager; // cau noi, thuc thi voi bucket nhung o tren redis
//    private static final long CAPACITY = 10;                  // tối đa 10 request
//    private static final Duration REFILL_INTERVAL = Duration.ofSeconds(10); // refill 10 token / 10s
//
//    public RedisRateLimitingFilter() {
//        // Kết nối Redis (dùng Lettuce)
//        RedisClient redisClient = RedisClient.create("redis://localhost:6379"); //demo
//        StatefulRedisConnection<String, byte[]> connection =
//                redisClient.connect(new io.lettuce.core.codec.Utf8StringCodec(), new io.lettuce.core.codec.ByteArrayCodec());
//
//        this.proxyManager = LettuceBasedProxyManager
//                .builderFor(connection)
//                .build();
//    }
//
        // nếu redis đã có key rồi thì lấy lên, nêếu chưa thì save vào, key là clientId, value là bucker dược seriallize
//    private Bucket resolveBucket(String clientId) {
//        Supplier<Bucket> bucketSupplier = () -> {
//            Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.intervally(CAPACITY, REFILL_INTERVAL));
//            return Bucket.builder().addLimit(limit).build();
//        };
//        return proxyManager.builder().build(clientId, bucketSupplier);
//    }
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        HttpServletRequest httpReq = (HttpServletRequest) request;
//        HttpServletResponse httpRes = (HttpServletResponse) response;
//
//        // Lấy IP hoặc header x-forwarded-for để xác định client
//        String clientIp = getClientIp(httpReq);
//        Bucket bucket = resolveBucket(clientIp);
//
//        if (bucket.tryConsume(1)) {
//            chain.doFilter(request, response);
//        } else {
//            httpRes.setStatus(429);
//            httpRes.setContentType("application/json");
//            httpRes.getWriter().write("{\"error\": \"Too Many Requests\"}");
//            httpRes.getWriter().flush();
//        }
//    }
//
//    private String getClientIp(HttpServletRequest request) {
//        String ip = request.getHeader("X-Forwarded-For");
//        if (ip == null || ip.isBlank()) {
//            ip = request.getRemoteAddr();
//        } else {
//            // Nếu có nhiều IP (qua proxy), lấy IP đầu tiên
//            ip = ip.split(",")[0];
//        }
//        return ip.trim();
//    }
//}
