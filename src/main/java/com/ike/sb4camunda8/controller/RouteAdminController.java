package com.ike.sb4camunda8.controller;

import com.ike.sb4camunda8.dto.CursorPage;
import com.ike.sb4camunda8.dto.DeployReq;
import com.ike.sb4camunda8.dto.SuppHttpMethod;
import com.ike.sb4camunda8.service.RouteRegisterService;
import com.ike.sb4camunda8.service.RoutesService;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.JobKind;
import io.camunda.client.api.search.response.Job;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 13/3/2026
 */
@RestController
@RequestMapping("/admin/routes")
public class RouteAdminController {

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
    public String deployBpmnXml(/*@RequestBody DeployReq req*/) {
        // 演示使用, 实际数据应通过请求传输
        var req = new DeployReq(
                "test",
                SuppHttpMethod.GET,
                "/testflow",
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:zeebe="http://camunda.org/schema/zeebe/1.0" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
                          <bpmn:process id="Process_1" isExecutable="true">
                            <bpmn:startEvent id="StartEvent_1">
                              <bpmn:outgoing>Flow_1hpujmn</bpmn:outgoing>
                            </bpmn:startEvent>
                            <bpmn:endEvent id="Event_01jyqc1">
                              <bpmn:incoming>Flow_1ydshe1</bpmn:incoming>
                            </bpmn:endEvent>
                            <bpmn:serviceTask id="Activity_07k2c3t">
                              <bpmn:extensionElements>
                                <zeebe:taskDefinition type="demo" />
                              </bpmn:extensionElements>
                              <bpmn:incoming>Flow_1hpujmn</bpmn:incoming>
                              <bpmn:outgoing>Flow_0mxyht5</bpmn:outgoing>
                            </bpmn:serviceTask>
                            <bpmn:sequenceFlow id="Flow_1hpujmn" sourceRef="StartEvent_1" targetRef="Activity_07k2c3t" />
                            <bpmn:sequenceFlow id="Flow_0mxyht5" sourceRef="Activity_07k2c3t" targetRef="Activity_0rvqxgc" />
                            <bpmn:serviceTask id="Activity_0rvqxgc">
                              <bpmn:extensionElements>
                                <zeebe:taskDefinition type="snowflake" />
                              </bpmn:extensionElements>
                              <bpmn:incoming>Flow_0mxyht5</bpmn:incoming>
                              <bpmn:outgoing>Flow_1wh46fl</bpmn:outgoing>
                            </bpmn:serviceTask>
                            <bpmn:sequenceFlow id="Flow_1ydshe1" sourceRef="Activity_1udcfk7" targetRef="Event_01jyqc1" />
                            <bpmn:serviceTask id="Activity_1udcfk7">
                              <bpmn:extensionElements>
                                <zeebe:taskDefinition type="test" />
                              </bpmn:extensionElements>
                              <bpmn:incoming>Flow_1wh46fl</bpmn:incoming>
                              <bpmn:outgoing>Flow_1ydshe1</bpmn:outgoing>
                            </bpmn:serviceTask>
                            <bpmn:sequenceFlow id="Flow_1wh46fl" sourceRef="Activity_0rvqxgc" targetRef="Activity_1udcfk7" />
                          </bpmn:process>
                          <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                            <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
                              <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
                                <dc:Bounds x="152" y="212" width="36" height="36" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Activity_1rwgdrm_di" bpmnElement="Activity_07k2c3t">
                                <dc:Bounds x="300" y="190" width="100" height="80" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Activity_1xi61r3_di" bpmnElement="Activity_0rvqxgc">
                                <dc:Bounds x="480" y="190" width="100" height="80" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Event_01jyqc1_di" bpmnElement="Event_01jyqc1">
                                <dc:Bounds x="862" y="212" width="36" height="36" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNShape id="Activity_0hw2q3u_di" bpmnElement="Activity_1udcfk7">
                                <dc:Bounds x="660" y="190" width="100" height="80" />
                              </bpmndi:BPMNShape>
                              <bpmndi:BPMNEdge id="Flow_1hpujmn_di" bpmnElement="Flow_1hpujmn">
                                <di:waypoint x="188" y="230" />
                                <di:waypoint x="300" y="230" />
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_0mxyht5_di" bpmnElement="Flow_0mxyht5">
                                <di:waypoint x="400" y="230" />
                                <di:waypoint x="480" y="230" />
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_1ydshe1_di" bpmnElement="Flow_1ydshe1">
                                <di:waypoint x="760" y="230" />
                                <di:waypoint x="862" y="230" />
                              </bpmndi:BPMNEdge>
                              <bpmndi:BPMNEdge id="Flow_1wh46fl_di" bpmnElement="Flow_1wh46fl">
                                <di:waypoint x="580" y="230" />
                                <di:waypoint x="660" y="230" />
                              </bpmndi:BPMNEdge>
                            </bpmndi:BPMNPlane>
                          </bpmndi:BPMNDiagram>
                        </bpmn:definitions>
                        """,
                true
        );
        var created = routesService.create(req);
        stringRedisTemplate.convertAndSend("route-event-update", created);

        // 向camunda部署工作流, 并在springboot应用中注册一个可以调用该工作流的自定义路由
/// FIXME: 为确保分布式一致性, 此处应该通过mq广播到每个节点
        routeRegisterService.register(created);

        return "Route registered: " + created.method() + " " + created.path();
    }

    @GetMapping
    public CursorPage<Job> search(String keyword, String after, String before, int size) {
        var resp = camundaClient
                .newJobSearchRequest()
                .filter(f -> {
                    f.kind(k -> k.eq(JobKind.BPMN_ELEMENT));
                    if (StringUtils.hasText(keyword)) {
                        f.worker(w -> w.like(keyword + "*"));
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