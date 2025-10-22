package com.nemi.config.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Wrapper cho HttpServletRequest để cache request body.
 * 
 * Vấn đề: HTTP request body stream chỉ có thể đọc 1 LẦN DUY NHẤT.
 * Nếu filter đọc body để verify HMAC, thì controller không thể đọc được nữa.
 * 
 * Giải pháp: Wrapper này đọc body 1 lần, cache lại trong memory,
 * sau đó cung cấp stream mới từ cache → có thể đọc nhiều lần.
 * 
 * Flow:
 * 1. Filter tạo CachedBodyHttpServletRequest(originalRequest)
 * 2. Constructor đọc hết stream gốc → lưu vào cachedBody
 * 3. Mọi lần gọi getInputStream() → trả về stream mới từ cachedBody
 * 4. Filter, Controller, Security đều có thể đọc body độc lập
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] cachedBody;

    /**
     * Tạo cached request wrapper và đọc toàn bộ body từ request gốc.
     * 
     * @param request HttpServletRequest gốc
     * @throws IOException nếu không đọc được body
     */
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // Đọc HẾT body stream từ request gốc VÀ cache lại
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    /**
     * Trả về ServletInputStream mới từ cached body.
     * Method này có thể gọi nhiều lần, mỗi lần trả về stream mới.
     * 
     * @return ServletInputStream có thể đọc cached body
     */
    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    /**
     * Trả về BufferedReader để đọc cached body dưới dạng text.
     * 
     * @return BufferedReader để đọc cached body
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    /**
     * Lấy raw bytes của cached body.
     * Trả về COPY của array để tránh modification từ bên ngoài.
     * 
     * @return byte array copy của request body
     */
    public byte[] getCachedBody() {
        return cachedBody.clone();
    }

    /**
     * ServletInputStream implementation đọc từ byte array đã cache.
     * Cho phép đọc body nhiều lần mà không cần đọc lại từ network.
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
