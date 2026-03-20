package com.ike.sb4camunda8.workers;

import com.ike.sb4camunda8.config.SnowflakeIdGenerator;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * <a href=https://docs.camunda.io/docs/apis-tools/camunda-spring-boot-starter/configuration/></>
 *
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 13/3/2026
 */
@Component
public class DemoWorker {
    private static final Logger log = LoggerFactory.getLogger(DemoWorker.class);
    private final RestTemplate restTemplate;

    public DemoWorker(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Operation(summary = "组件demo")
    @JobWorker(type = "demo")
    public Map<String, Object> demo(@Parameter(description = "参数keyA") @Variable(name = "keyA") String keyA) {
        log.info("Hello World: {}", keyA);
        String newVal = "paramA";
        return Map.of("keyA", newVal);
    }

    @Operation(summary = "生成雪花ID")
    @JobWorker(type = "snowflake")
    public Map<String, Object> genSnowflakeId(@Parameter(description = "自定义key1") @Variable(name = "key1") String key1) {
        SnowflakeIdGenerator instance = SnowflakeIdGenerator.getInstance();
        System.out.println("KEY1 ====" + key1);
        long id = instance.generateId();
        log.info("Snowflake: {}", id);
        return Map.of("keyC", id);
    }

    @Operation(summary = "组件test")
    @JobWorker(type = "test")
    public Map<String, Object> test(@Parameter(description = "自定义headers") @Variable("headers") Map<String, Object> headers) {
        log.info("test: {}", headers);
        headers.put("hkey2", 12321321);
        return Map.of("headers", headers);
    }

    @Operation(summary = "请求api")
    @JobWorker(type = "request-api")
    public void handleJobFoo() {
        var resp = restTemplate.getForEntity("https://www.github.com", String.class);
        String body = resp.getBody();
        log.info(body);
    }
}
