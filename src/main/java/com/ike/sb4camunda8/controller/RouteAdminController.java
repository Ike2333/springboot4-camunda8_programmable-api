package com.ike.sb4camunda8.controller;

import com.ike.sb4camunda8.dto.CursorPage;
import com.ike.sb4camunda8.dto.DeployReq;
import com.ike.sb4camunda8.dto.SuppHttpMethod;
import com.ike.sb4camunda8.service.RouteRegisterService;
import com.ike.sb4camunda8.service.RoutesService;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.JobKind;
import io.camunda.client.api.search.response.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 13/3/2026
 */
@RestController
@RequestMapping("/admin/routes")
public class RouteAdminController {
    private static final Logger log = LoggerFactory.getLogger(RouteAdminController.class);
    private final CamundaClient camundaClient;
    private final RoutesService routesService;
    private final RouteRegisterService routeRegisterService;
    private final StringRedisTemplate stringRedisTemplate;

    public RouteAdminController(CamundaClient camundaClient, RoutesService routesService, RouteRegisterService routeRegisterService, StringRedisTemplate stringRedisTemplate) {
        this.camundaClient = camundaClient;
        this.routesService = routesService;
        this.routeRegisterService = routeRegisterService;
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @PostMapping("/deploy")
    public String deployBpmnXml(
//            @RequestBody @Validated DeployReq req
    ) {
        // 演示使用, 实际数据应通过请求传输
        var req = new DeployReq(
                "test",
                SuppHttpMethod.PUT,
                "/testflow",
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:zeebe="http://camunda.org/schema/zeebe/1.0" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
                          <bpmn:process id="Process_1" isExecutable="true">
                            <bpmn:startEvent id="Event_13azmbm">
                              <bpmn:outgoing>Flow_15yzj3b</bpmn:outgoing>
                            </bpmn:startEvent>
                            <bpmn:intermediateThrowEvent id="Event_06ei2c6">
                              <bpmn:incoming>Flow_1dn69nu</bpmn:incoming>
                            </bpmn:intermediateThrowEvent>
                            <bpmn:sequenceFlow id="Flow_15yzj3b" sourceRef="Event_13azmbm" targetRef="Activity_0oyl5vp" />
                            <bpmn:sequenceFlow id="Flow_1dn69nu" sourceRef="Activity_0oyl5vp" targetRef="Event_06ei2c6" />
                            <bpmn:serviceTask id="Activity_0oyl5vp">
                              <bpmn:extensionElements>
                                <zeebe:taskDefinition type="snowflake" />
                                <zeebe:ioMapping>
                                  <zeebe:input source="=12312313212323" target="key1" />
                                </zeebe:ioMapping>
                              </bpmn:extensionElements>
                              <bpmn:incoming>Flow_15yzj3b</bpmn:incoming>
                              <bpmn:outgoing>Flow_1dn69nu</bpmn:outgoing>
                            </bpmn:serviceTask>
                          </bpmn:process>
                          <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                            <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
                              <bpmndi:BPMNShape id="Event_13azmbm_di" bpmnElement="Event_13azmbm">
                                <dc:Bounds x="152" y="112" width="36" height="36" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Event_06ei2c6_di" bpmnElement="Event_06ei2c6">
                                <dc:Bounds x="532" y="112" width="36" height="36" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Activity_1u6wacj_di" bpmnElement="Activity_0oyl5vp">
                                <dc:Bounds x="300" y="90" width="100" height="80" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNEdge id="Flow_15yzj3b_di" bpmnElement="Flow_15yzj3b">
                                <di:waypoint x="188" y="130" />
                                <di:waypoint x="300" y="130" />
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_1dn69nu_di" bpmnElement="Flow_1dn69nu">
                                <di:waypoint x="400" y="130" />
                                <di:waypoint x="532" y="130" />
                              </bpmndi:BPMNEdge>
                            </bpmndi:BPMNPlane>
                          </bpmndi:BPMNDiagram>
                        </bpmn:definitions>
                        """,
                true
        );

        Assert.isTrue(!req.path().startsWith("/routes"), () -> "此路径不允许使用");
        var created = routesService.create(req);
        stringRedisTemplate.convertAndSend("route-event-update", created);

        // 向camunda部署工作流, 并在springboot应用中注册一个可以调用该工作流的自定义路由
        try {
            routeRegisterService.register(created);
        } catch (Exception e) {
            routesService.turnoff(created.id());
            log.error("当前流程无法运行 [{}]", created.name(), e);
        }

        return "Route registered: " + created.method() + " " + created.path();
    }

    /**
     * 分页查询流程运行日志
     * @param keyword 流程名称关键字(前模糊)
     * @param after 在此游标之前
     * @param before 在此游标之后
     * @param size 每页大小
     * @param endTimeBefore 在此时间之前
     * @param endTimeAfter 在此时间之后
     * @return 流程运行日志
     */
    @GetMapping
    public CursorPage<Job> search(String keyword, String after, String before, int size, Instant endTimeBefore, Instant endTimeAfter) {
        var resp = camundaClient
                .newJobSearchRequest()
                .filter(f -> {
                    f.kind(k -> k.eq(JobKind.BPMN_ELEMENT));
                    if (StringUtils.hasText(keyword)) {
                        f.worker(w -> w.like(keyword + "*"));
                    }
                    if (endTimeAfter!= null) {
                        f.endTime(t -> t.gte(OffsetDateTime.from(endTimeAfter)));
                    }

                    if (endTimeBefore!= null) {
                        f.endTime(t -> t.gte(OffsetDateTime.from(endTimeBefore)));
                    }
                })
                .page(p -> {
                    // 本质上是查询elastic search, 需使用游标分页
                    p.limit(size);
                    if (StringUtils.hasText(after)) {
                        p.after(after);
                    }
                    if (StringUtils.hasText(before)) {
                        p.before(before);
                    }
                })
                .send()
                .join();

        return new CursorPage<>(resp.items(), resp.page().startCursor(), resp.page().endCursor());
    }
}