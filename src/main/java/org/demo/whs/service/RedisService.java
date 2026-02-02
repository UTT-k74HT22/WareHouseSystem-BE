package org.demo.whs.service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface RedisService {
    /**
     * Lưu key-value với TTL vào Redis
     *
     * @param key       Redis key
     * @param value     Redis value
     * @param ttl       Thời gian sống (timeout)
     * @param timeUnit  Đơn vị thời gian (SECONDS, MINUTES, HOURS...)
     */
    void set(String key, Object value, long ttl, TimeUnit timeUnit);

    /**
     * Lấy giá trị từ Redis theo key
     *
     * @param key Redis key
     * @return Object hoặc null nếu không tồn tại
     */
    Object get(String key);

    /**
     * Lấy và convert về kiểu mong muốn
     *
     * @param key   Redis key
     * @param clazz Kiểu dữ liệu mong muốn
     * @param <T>   Generic type
     * @return Optional<T>, empty nếu không có dữ liệu
     */
    <T> Optional<T> get(String key, Class<T> clazz);

    /**
     * Kiểm tra key có tồn tại trong Redis hay không
     *
     * @param key Redis key
     * @return true nếu tồn tại, false nếu không
     */
    boolean exists(String key);

    /**
     * Xóa key trong Redis
     *
     * @param key Redis key
     */
    void delete(String key);
}
