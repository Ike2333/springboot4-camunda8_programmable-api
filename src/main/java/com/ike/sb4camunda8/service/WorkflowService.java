package com.ike.sb4camunda8.service;

import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 13/3/2026
 */
@FunctionalInterface
public interface WorkflowService {
    ServerResponse startWorkflow(ServerRequest request, String processId) throws Exception;
}
