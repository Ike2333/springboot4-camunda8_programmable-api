package com.ike.sb4camunda8.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 14/3/2026
 */
@Schema(description = "游标分页结果封装")
public record CursorPage<T>(
        @Schema(description = "数据列表")
        List<T> content,
        @Schema(description = "本页起始游标（用于向上翻页）")
        String after,
        @Schema(description = "本页结束游标（用于向下翻页）")
        String before
) {
}
