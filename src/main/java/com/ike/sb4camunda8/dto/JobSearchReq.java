package com.ike.sb4camunda8.dto;

import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 20/3/2026
 */
public record JobSearchReq(
        // 关键字
        @Nullable String keyword,
        // 在此游标之前
        @Nullable String before,
        // 在此游标之后
        @Nullable String after,
        // 每页大小
        @Min(1) Integer size,
        // 在此时间之前
        @Nullable Instant endTimeBefore,
        // 在此时间之后
        @Nullable Instant endTimeAfter
) {
}
