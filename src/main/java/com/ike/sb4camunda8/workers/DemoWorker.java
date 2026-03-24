package com.ike.sb4camunda8.workers;

import com.ike.sb4camunda8.config.SnowflakeIdGenerator;
import com.ike.sb4camunda8.dto.DeployReq;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

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


    public static void main(String[] args) {
        String json = """
                {
                    "name": "ces",
                    "method": "POST",
                    "path": "/ces",
                    "bpmnXml": "<?xml version=\\"1.0\\" encoding=\\"UTF-8\\"?>\\n<bpmn:definitions xmlns:xsi=\\"http://www.w3.org/2001/XMLSchema-instance\\" xmlns:bpmn=\\"http://www.omg.org/spec/BPMN/20100524/MODEL\\" xmlns:bpmndi=\\"http://www.omg.org/spec/BPMN/20100524/DI\\" xmlns:dc=\\"http://www.omg.org/spec/DD/20100524/DC\\" xmlns:di=\\"http://www.omg.org/spec/DD/20100524/DI\\" targetNamespace=\\"http://bpmn.io/schema/bpmn\\">\\n    <bpmn:process id=\\"aaa_aaa\\" isExecutable=\\"true\\">\\n      <bpmn:startEvent id=\\"Event_0r9etry\\">\\n        <bpmn:outgoing>Flow_1r5q9gy</bpmn:outgoing>\\n      </bpmn:startEvent>\\n      <bpmn:intermediateThrowEvent id=\\"Event_15iie8i\\">\\n        <bpmn:incoming>Flow_0cnnkao</bpmn:incoming>\\n      </bpmn:intermediateThrowEvent>\\n      <bpmn:sequenceFlow id=\\"Flow_1r5q9gy\\" sourceRef=\\"Event_0r9etry\\" targetRef=\\"aaa\\" />\\n      <bpmn:sequenceFlow id=\\"Flow_0cnnkao\\" sourceRef=\\"aaa\\" targetRef=\\"Event_15iie8i\\" />\\n      <bpmn:serviceTask id=\\"aaa\\">\\n        <bpmn:incoming>Flow_1r5q9gy</bpmn:incoming>\\n        <bpmn:outgoing>Flow_0cnnkao</bpmn:outgoing>\\n      </bpmn:serviceTask>\\n    </bpmn:process>\\n    <bpmndi:BPMNDiagram id=\\"BPMNDiagram_1\\">\\n      <bpmndi:BPMNPlane id=\\"BPMNPlane_1\\" bpmnElement=\\"aaa_aaa\\">\\n        <bpmndi:BPMNShape id=\\"Event_0r9etry_di\\" bpmnElement=\\"Event_0r9etry\\">\\n          <dc:Bounds x=\\"362\\" y=\\"212\\" width=\\"36\\" height=\\"36\\" />\\n        </bpmndi:BPMNShape>\\n        <bpmndi:BPMNShape id=\\"Event_15iie8i_di\\" bpmnElement=\\"Event_15iie8i\\">\\n          <dc:Bounds x=\\"792\\" y=\\"212\\" width=\\"36\\" height=\\"36\\" />\\n        </bpmndi:BPMNShape>\\n        <bpmndi:BPMNShape id=\\"Activity_1vrp57p_di\\" bpmnElement=\\"aaa\\">\\n          <dc:Bounds x=\\"540\\" y=\\"190\\" width=\\"100\\" height=\\"80\\" />\\n        </bpmndi:BPMNShape>\\n        <bpmndi:BPMNEdge id=\\"Flow_1r5q9gy_di\\" bpmnElement=\\"Flow_1r5q9gy\\">\\n          <di:waypoint x=\\"398\\" y=\\"230\\" />\\n          <di:waypoint x=\\"540\\" y=\\"230\\" />\\n        </bpmndi:BPMNEdge>\\n        <bpmndi:BPMNEdge id=\\"Flow_0cnnkao_di\\" bpmnElement=\\"Flow_0cnnkao\\">\\n          <di:waypoint x=\\"640\\" y=\\"230\\" />\\n          <di:waypoint x=\\"792\\" y=\\"230\\" />\\n        </bpmndi:BPMNEdge>\\n      </bpmndi:BPMNPlane>\\n    </bpmndi:BPMNDiagram>\\n</bpmn:definitions>"
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        DeployReq req = objectMapper.readValue(json, DeployReq.class);
        System.out.println(req.bpmnXml());
    }
}
