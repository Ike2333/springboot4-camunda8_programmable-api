package com.ike.sb4camunda8.controller;

import com.ike.sb4camunda8.dto.CursorPage;
import com.ike.sb4camunda8.dto.DeployReq;
import com.ike.sb4camunda8.dto.JobSearchReq;
import com.ike.sb4camunda8.dto.RoutesDto;
import com.ike.sb4camunda8.service.RouteRegisterService;
import com.ike.sb4camunda8.service.RoutesService;
import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.search.enums.JobKind;
import io.camunda.client.api.search.response.Job;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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


    /**
     * 部署API
     */
    @PostMapping("/deploy")
    public ResponseEntity<Map<String, String>> deployBpmnXml(@RequestBody @Validated DeployReq req) {
        Assert.isTrue(!req.path().startsWith("/routes"), () -> "此路径不允许使用");
        var created = routesService.create(req);

        // 向camunda部署工作流, 并在springboot应用中注册一个可以调用该工作流的自定义路由
        try {
            routeRegisterService.register(created);
        } catch (Exception e) {
            routesService.updateEnableById(false, created.id());
            log.error("当前流程无法运行 [{}]", created.name(), e);
        }
        stringRedisTemplate.convertAndSend("route-event-update", created);

        return ResponseEntity.ok(Map.of("", "Route registered: " + created.method() + " " + created.path()));
    }

    /**
     * 注销流程 - 停用并从数据库删除
     *
     * @param id 流程ID
     */
    @DeleteMapping("/deploy/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        routesService.cancel(id);
        return ResponseEntity.ok().build();
    }


    /**
     * 根据流程ID注销接口
     *
     * @param keyword: 关键字, 前模糊匹配name和path
     */
    @GetMapping("/deploy")
    public ResponseEntity<Page<RoutesDto>> findAllDeployedRoutes(String keyword, Pageable pageable) {
        return ResponseEntity.ok(routesService.findAll(keyword, pageable));
    }


    /**
     * 分页查询流程运行日志
     *
     * @return 流程运行日志
     */
    @GetMapping
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