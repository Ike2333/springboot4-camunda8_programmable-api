package com.ike.sb4camunda8.controller;

import com.ike.sb4camunda8.dto.*;
import com.ike.sb4camunda8.service.RouteRegisterService;
import com.ike.sb4camunda8.service.RoutesService;
import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.client.api.search.enums.JobKind;
import io.camunda.client.api.search.response.Job;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 13/3/2026
 */
@Tag(name = "部署管理", description = "负责 BPMN 流程文件的部署, 撤回及路由映射维护")
@RestController
@RequestMapping("/admin/routes")
public class RouteAdminController {
    private static final Logger log = LoggerFactory.getLogger(RouteAdminController.class);
    private final CamundaClient camundaClient;
    private final RoutesService routesService;
    private final ApplicationContext applicationContext;
    private final RouteRegisterService routeRegisterService;
    private final StringRedisTemplate stringRedisTemplate;

    public RouteAdminController(CamundaClient camundaClient, RoutesService routesService, ApplicationContext applicationContext, RouteRegisterService routeRegisterService, StringRedisTemplate stringRedisTemplate) {
        this.camundaClient = camundaClient;
        this.routesService = routesService;
        this.applicationContext = applicationContext;
        this.routeRegisterService = routeRegisterService;
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Tag(name = "worker查询", description = "查询camunda中可用的workers")
    @Operation(
            summary = "部署 BPMN XML 并注册路由",
            description = "接收 BPMN 定义并将其转换为动态 API 路由. 注意: 路径不能以 '/routes' 开头",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "部署成功",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                            {"message": "Route registered: POST /api/v1/process"}
                                            """))
                    ),
                    @ApiResponse(responseCode = "400", description = "请求参数非法或路径被禁止"),
                    @ApiResponse(responseCode = "500", description = "服务器内部错误")
            }
    )
    @GetMapping("/workers")
    public ResponseEntity<List<Map<String, Object>>> getWorkers() {
        List<Map<String, Object>> workerList = new ArrayList<>();
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        for (Object bean : beans.values()) {
            // 处理 AOP 代理问题, 当存在 @Transactional 时确保拿到原 Class
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(JobWorker.class)) {
                    workerList.add(extractWorkerMetadata(method));
                }
            }
        }
        return ResponseEntity.ok(workerList);
    }

    private Map<String, Object> extractWorkerMetadata(Method method) {
        Map<String, Object> metadata = new HashMap<>();

        // 读取 JobWorker
        JobWorker jobWorker = method.getAnnotation(JobWorker.class);
        metadata.put("workerType", jobWorker.type());
        metadata.put("methodName", method.getName());

        // 读取 Operation
        if (method.isAnnotationPresent(Operation.class)) {
            Operation op = method.getAnnotation(Operation.class);
            metadata.put("summary", op.summary());
            metadata.put("description", op.description());
        }

        List<Map<String, Object>> parameters = new ArrayList<>();
        for (java.lang.reflect.Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(Variable.class)) {
                parameters.add(extractParameterMetadata(param));
            }
        }
        metadata.put("variables", parameters);

        return metadata;
    }

    private Map<String, Object> extractParameterMetadata(java.lang.reflect.Parameter param) {
        Map<String, Object> pMap = new HashMap<>();

        // 获取 @Variable 变量名
        Variable varAnn = param.getAnnotation(Variable.class);
        String varName = varAnn.name().isEmpty() ? varAnn.value() : varAnn.name();
        pMap.put("name", varName.isEmpty() ? param.getName() : varName);

        // 获取参数类型, 包括泛型类型
        pMap.put("type", param.getParameterizedType().getTypeName());
        pMap.put("simpleTypeName", param.getType().getSimpleName());

        // 读取 @Parameter
        if (param.isAnnotationPresent(Parameter.class)) {
            Parameter pAnn = param.getAnnotation(Parameter.class);
            pMap.put("description", pAnn.description());
            pMap.put("required", pAnn.required());
        }

        return pMap;
    }


    @PostMapping("/debug")
    public void debug() {
        String xml = """
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
                        <di:waypoint x="536" y="130" />
                      </bpmndi:BPMNEdge>
                    </bpmndi:BPMNPlane>
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """;
        DeploymentEvent join = camundaClient.newDeployResourceCommand().addResourceBytes(xml.getBytes(), "debug.bpmn").send().join();
        long key = join.getKey();
        log.info("deploy key: {}", key);
        String bpmnProcessId = join.getProcesses().getFirst().getBpmnProcessId();
        log.info("bpmnProcessId: {}", bpmnProcessId);
        long processDefinitionKey = join.getProcesses().getFirst().getProcessDefinitionKey();
        log.info("processDefinitionKey: {}", processDefinitionKey);
        int version = join.getProcesses().getFirst().getVersion();
        log.info("version: {}", version);

        var instance = camundaClient
                .newCreateInstanceCommand()
                .bpmnProcessId(bpmnProcessId)
                .latestVersion()
                .withResult()
                .send()
                .join();

        long processDefinitionKey1 = instance.getProcessDefinitionKey();
        long processInstanceKey = instance.getProcessInstanceKey();
        String bpmnProcessId1 = instance.getBpmnProcessId();
        int version1 = instance.getVersion();
        log.info("new definitionKey: {}", processDefinitionKey1);
        log.info("new processInstanceKey: {}", processInstanceKey);
        log.info("new bpmnProcessId1: {}", bpmnProcessId1);
        log.info("new version1: {}", version1);

        System.out.println(camundaClient.newProcessDefinitionGetXmlRequest(processDefinitionKey1).send().join());

    }

    /**
     * 部署API
     */
    @Tag(name = "流程部署", description = "动态路由与 BPMN 资源的注册管理")
    @Operation(
            summary = "部署 BPMN XML 并注册路由",
            description = "接收 BPMN 定义并将其转换为动态 API 路由. 注意: 路径不能以 '/routes' 开头",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "部署成功",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                            {"message": "Route registered: POST /api/v1/process"}
                                            """)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "请求参数非法或路径被禁止"),
                    @ApiResponse(responseCode = "500", description = "服务器内部错误")
            }
    )
    @PostMapping("/deploy")
    public ResponseEntity<Map<String, String>> deployBpmnXml(@RequestBody @Validated DeployReq req) {
        Assert.isTrue(!req.path().startsWith("/routes"), () -> "此路径不允许使用");
        var created = routesService.create(req);

        return ResponseEntity.ok(Map.of("message", "Route registered: " + created.method() + " " + created.path()));
    }


    @Tag(name = "根据路由ID查询路由, 同时返回XML", description = "根据ID获取包含BPMN XML定义的路由信息")
    @GetMapping("/deploy/{id}")
    public ResponseEntity<RouteWithBpmn> get(@Parameter(description = "路由唯一主键 ID", example = "1001") @PathVariable Long id) {
        return ResponseEntity.ok(routesService.getById(id));
    }

    /**
     * 注销流程 - 停用并从数据库删除
     *
     * @param id 路由ID
     */
    @Operation(summary = "注销/删除流程路由", description = "根据 ID 彻底删除路由定义并停用相关服务")
    @ApiResponse(responseCode = "200", description = "注销成功")
    @ApiResponse(responseCode = "404", description = "路由 ID 不存在")
    @DeleteMapping("/deploy/{id}")
    public ResponseEntity<Void> cancelRouteById(@Parameter(description = "路由唯一主键 ID", example = "1001") @PathVariable Long id) {
        routesService.cancel(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 设置路由状态为停用
     *
     * @param id 路由ID
     */
    @Operation(summary = "停用路由", description = "将路由状态修改为‘停用’，暂时禁止通过该路径访问")
    @PutMapping("/deploy/{id}/stop")
    public ResponseEntity<Void> stop(@Parameter(description = "路由唯一主键 ID") @PathVariable Long id) {
        return ResponseEntity.ok().build();
    }

    /**
     * 设置路由状态为启用
     *
     * @param id 路由ID
     */
    @PutMapping("/deploy/{id}/start")
    @Operation(summary = "启用路由", description = "将路由状态恢复为‘启用’")
    public ResponseEntity<Void> start(@Parameter(description = "路由唯一主键 ID") @PathVariable Long id) {
        return ResponseEntity.ok().build();
    }


    /**
     * 分页查询路由
     *
     * @param keyword: 关键字, 前模糊匹配name和path
     */
    @GetMapping("/deploy")
    @Operation(summary = "分页查询已部署路由", description = "支持根据名称(name)或路径(path)进行前模糊匹配查询")
    public ResponseEntity<Page<RoutesDto>> findAllDeployedRoutes(
            @Parameter(description = "搜索关键字（匹配名称或路径）", example = "order", required = false) String keyword,
            @Parameter(hidden = true) Pageable pageable
    ) {
        return ResponseEntity.ok(routesService.findAll(keyword, pageable));
    }


    /**
     * 分页查询流程运行日志
     *
     * @return 流程运行日志
     */
    @GetMapping("/job")
    @Operation(
            summary = "分页搜索作业(Job)",
            description = "基于 Camunda 引擎的作业查询。由于底层对接 ElasticSearch，采用游标分页方式，支持按关键字和结束时间范围过滤。"
    )
    @ApiResponse(responseCode = "200", description = "返回带游标的分页数据")
    public ResponseEntity<CursorPage<Job>> search(@ParameterObject @Validated JobSearchReq req) {
        var resp = camundaClient
                .newJobSearchRequest()
                .filter(f -> {
                    f.kind(k -> k.eq(JobKind.BPMN_ELEMENT));
                    if (StringUtils.hasText(req.keyword())) {
                        f.worker(w -> w.like(req.keyword() + "*"));
                    }
                    if (req.endTimeAfter() != null) {
                        f.endTime(t -> t.gte(OffsetDateTime.from(req.endTimeAfter())));
                    }

                    if (req.endTimeBefore() != null) {
                        f.endTime(t -> t.gte(OffsetDateTime.from(req.endTimeBefore())));
                    }
                })
                .page(p -> {
                    // 本质上是查询elastic search, 需使用游标分页
                    p.limit(req.size());
                    if (StringUtils.hasText(req.after())) {
                        p.after(req.after());
                    }
                    if (StringUtils.hasText(req.before())) {
                        p.before(req.before());
                    }
                })
                .send()
                .join();

        return ResponseEntity.ok(new CursorPage<>(resp.items(), resp.page().startCursor(), resp.page().endCursor()));
    }
}