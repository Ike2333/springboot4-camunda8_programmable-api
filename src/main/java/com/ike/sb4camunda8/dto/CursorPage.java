package com.ike.sb4camunda8.dto;

import java.util.List;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 14/3/2026
 */
public record CursorPage<T>(
        List<T> content,
        String after,
        String before
) {
}
