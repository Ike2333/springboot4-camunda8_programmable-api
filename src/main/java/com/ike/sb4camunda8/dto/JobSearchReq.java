package com.ike.sb4camunda8.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 20/3/2026
 */
@Schema(description = "作业搜索请求参数")
public record JobSearchReq(
        // 关键字
        @Schema(description = "搜索关键字：模糊匹配 Worker 字段", example = "order-processor")
        @Nullable String keyword,
        // 在此游标之前
        @Schema(description = "向上翻页游标：获取此游标之前的数据")
        @Nullable String before,
        // 在此游标之后
        @Schema(description = "向下翻页游标：获取此游标之后的数据")
        @Nullable String after,
        // 每页大小
        @Schema(description = "页大小", defaultValue = "10", example = "20")
        @Min(1) Integer size,
        // 在此时间之前
        @Schema(description = "结束时间上限：在此时间戳之前结束的作业 (ISO-8601)", example = "2024-05-01T10:00:00Z")
        @Nullable Instant endTimeBefore,
        // 在此时间之后
        @Schema(description = "结束时间下限：在此时间戳之后结束的作业 (ISO-8601)", example = "2024-04-01T10:00:00Z")
        @Nullable Instant endTimeAfter
) {
}
