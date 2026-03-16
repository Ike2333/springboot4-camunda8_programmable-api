package com.ike.sb4camunda8.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

/**
 * optimized for virtual threads
 *
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 13/3/2026
 */
public class SnowflakeIdGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    private static final long EPOCH = LocalDateTime.of(2025, 1, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long MACHINE_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATACENTER_ID_BITS;

    private final long datacenterId;
    private final long machineId;
    private final AtomicLong state;


    public SnowflakeIdGenerator(long datacenterId, long machineId) {
        if (datacenterId < 0 || datacenterId >= (1 << DATACENTER_ID_BITS)) {
            throw new IllegalArgumentException("datacenterId out of range");
        }

        if (machineId < 0 || machineId >= (1 << MACHINE_ID_BITS)) {
            throw new IllegalArgumentException("machineId out of range");
        }

        this.datacenterId = datacenterId;
        this.machineId = machineId;
        this.state = new AtomicLong();

        logger.info("[SnowflakeIdGenerator] init success datacenterId={} machineId={}", datacenterId, machineId);
    }

    private static class Holder {
        private static final SnowflakeIdGenerator INSTANCE = new SnowflakeIdGenerator(1, 1);
    }

    public static SnowflakeIdGenerator getInstance() {
        return Holder.INSTANCE;
    }

    public long generateId() {
        while (true) {
            long current = state.get();
            long lastTimestamp = current >>> SEQUENCE_BITS;
            long sequence = current & MAX_SEQUENCE;
            long now = currentTime();
            if (now < lastTimestamp) {
                // 时钟回拨，等待
                now = waitUntil(lastTimestamp);
            }

            long newTimestamp;
            long newSequence;

            if (now == lastTimestamp) {
                newSequence = (sequence + 1) & MAX_SEQUENCE;
                if (newSequence == 0) {
                    now = waitUntil(lastTimestamp);
                    newTimestamp = now;
                } else {
                    newTimestamp = lastTimestamp;
                }
            } else {
                newTimestamp = now;
                newSequence = 0;
            }

            long newState = (newTimestamp << SEQUENCE_BITS) | newSequence;

            if (state.compareAndSet(current, newState)) {
                return ((newTimestamp - EPOCH) << TIMESTAMP_SHIFT)
                        | (datacenterId << DATACENTER_ID_SHIFT)
                        | (machineId << MACHINE_ID_SHIFT)
                        | newSequence;
            }
        }
    }

    private long waitUntil(long lastTimestamp) {
        long now;
        do {
            Thread.onSpinWait();
            now = currentTime();
        } while (now <= lastTimestamp);

        return now;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }

}
