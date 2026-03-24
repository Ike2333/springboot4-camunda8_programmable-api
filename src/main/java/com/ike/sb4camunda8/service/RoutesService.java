package com.ike.sb4camunda8.service;

import com.ike.sb4camunda8.dto.*;
import com.ike.sb4camunda8.entity.Routes;
import com.ike.sb4camunda8.mappers.EntityDtoMapper;
import com.ike.sb4camunda8.repository.RoutesRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.xml.XmlMapper;

import java.util.NoSuchElementException;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
@Service
public class RoutesService {

    private final RoutesRepository routesRepository;
    private final EntityDtoMapper entityDtoMapper;
    private final RouteRegisterService routeRegisterService;
    private final XmlMapper xmlMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public RoutesService(RoutesRepository routesRepository, EntityDtoMapper entityDtoMapper, RouteRegisterService routeRegisterService, XmlMapper xmlMapper, StringRedisTemplate stringRedisTemplate) {
        this.routesRepository = routesRepository;
        this.entityDtoMapper = entityDtoMapper;
        this.routeRegisterService = routeRegisterService;
        this.xmlMapper = xmlMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional
    public RoutesDto create(DeployReq req) {
        var processId = validateBpmnXmlAndExtractId(req.bpmnXml());
        boolean b = routesRepository.existsByBpmnProcessId(processId);
        if (b) {
            throw new IllegalArgumentException("流程创建失败, Process ID 已存在: " + processId);
        }
        CamundaDeployResp resp = routeRegisterService.register(req);
        Assert.isTrue(processId.equals(resp.processId()), () -> "ProcessID 不匹配: " + resp.processId() + "===" + processId);
        var builtRoute = new Routes(
                req.name(),
                req.method(),
                req.path(),
                resp.processId(),
                resp.processDefinitionKey(),
                resp.version(),
                true
        );
        var saved = routesRepository.save(builtRoute);
        // 向camunda部署工作流, 并在springboot应用中注册一个可以调用该工作流的自定义路由
        stringRedisTemplate.convertAndSend("route-event-update", req);
        return entityDtoMapper.convertRoutesToRoutesDto(saved);
    }

    private String validateBpmnXmlAndExtractId(String bpmnXml) {
        JsonNode rootNode = xmlMapper.readTree(bpmnXml);
        JsonNode processNode = rootNode.get("process");
        Assert.isTrue(processNode != null, () -> "BPMN不合法: 未找到process节点");
        String id;
        boolean isExecutable;
        try {
            id = processNode.get("id").asString();
            isExecutable = processNode.get("isExecutable").asBoolean();
        } catch (Exception e) {
            throw new IllegalArgumentException("BPMN不合法: 缺少id或isExecutable节点");
        }
        Assert.isTrue(StringUtils.hasText(id) && isExecutable, () -> "BPMN不合法: id为空或isExecutable为false");
        return id;
    }


    public Page<RoutesDto> findAll(String keyword, Pageable pageable) {
        Specification<Routes> spec = (root, query, cb) -> {
            if (StringUtils.hasText(keyword)) {
                Predicate name = cb.like(root.get("name"), keyword + "%");
                Predicate path = cb.like(root.get("path"), keyword + "%");
                return cb.or(name, path);
            }
            return cb.conjunction();
        };
        return routesRepository.findAll(spec, pageable).map(entityDtoMapper::convertRoutesToRoutesDto);
    }


    @Transactional
    public void cancel(Long id) {
        var entity = routesRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Route not found by ID: " + id));
        RoutesDto dto = entityDtoMapper.convertRoutesToRoutesDto(entity);
        routeRegisterService.cancel(dto);
        routesRepository.delete(entity);
    }


    public RouteWithBpmn getById(Long id) {
        var r = findById(id);
        RouteWithBpmn routeWithBpmn = entityDtoMapper.convertRouteEntityToBpmnStruct(r);
        Long processDefinitionKey = r.getProcessDefinitionKey();
        String xml = routeRegisterService.findByProcessDefinitionKey(processDefinitionKey);
        routeWithBpmn.setBpmnXml(xml);
        return routeWithBpmn;
    }


    public Routes findById(Long id) {
        return routesRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Route not found by ID: " + id));
    }


    @Transactional
    public void stop(Long id) {
        var r = findById(id);
        RoutesDto routesDto = entityDtoMapper.convertRoutesToRoutesDto(r);
        routeRegisterService.cancel(routesDto);
        r.setActive(false);
        routesRepository.save(r);
    }


    @Transactional
    public void start(Long id) {
        var r = findById(id);
        CamundaDeployResp resp = getCamundaDeployResp(r);
        Long newDefinitionKey = resp.processDefinitionKey();
        Integer version = resp.version();
        routesRepository.updateProcessDefinitionKeyAndVersionById(newDefinitionKey, version, r.getId());
    }

    private CamundaDeployResp getCamundaDeployResp(Routes r) {
        Long processDefinitionKey = r.getProcessDefinitionKey();
        String bpmnXml = routeRegisterService.findByProcessDefinitionKey(processDefinitionKey);
        Assert.isTrue(StringUtils.hasText(bpmnXml), () -> "BPMN XML 不存在, 当前 process definition key: " + processDefinitionKey);
        var req = new DeployReq(r.getName(), r.getMethod(), r.getPath(), bpmnXml);
        return routeRegisterService.register(req);
    }


    public RouteWithBpmn changeVersion(ChangeVersionReq req) {
        Routes r = findById(req.id());
        Integer latestVersionNumUseProcessId = routeRegisterService.getLatestVersionNumUseProcessId(r.getBpmnProcessId());
        Assert.isTrue(req.version() > 0 && req.version() <= latestVersionNumUseProcessId, () -> "版本不支持: " + req.version());

        // 根据 Process ID和 version 获取 process definition
        var definition = routeRegisterService.fetchXmlUseProcessIdAndVersion(r.getBpmnProcessId(), req.version());

        // 更新版本号
        r.setVersion(definition.getVersion());
        r.setProcessDefinitionKey(definition.getProcessDefinitionKey());
        Routes saved = routesRepository.save(r);
        // 更新注册的流程版本
        CamundaDeployResp resp = getCamundaDeployResp(saved);
        // 根据definitionKey获取BPMN XML
        RouteWithBpmn routeWithBpmn = entityDtoMapper.convertRouteEntityToBpmnStruct(saved);
        routeWithBpmn.setBpmnXml(resp.bpmnXml());

        return routeWithBpmn;
    }


    @Transactional
    public RouteWithBpmn updateRoute(Long id, DeployReq req) {
        Routes route = findById(id);
        String processId = validateBpmnXmlAndExtractId(req.bpmnXml());
        Assert.isTrue(processId.equals(route.getBpmnProcessId()), () -> "修改失败, BPMN XML中的ProcessID不匹配, 期望值: " + route.getBpmnProcessId() + ", 实际值: " + processId);

        CamundaDeployResp registered = routeRegisterService.register(req);
        // 更新数据库中的version/processDefinitionKey
        Integer version = registered.version();
        Long processDefinitionKey = registered.processDefinitionKey();
        route.setVersion(version);
        route.setProcessDefinitionKey(processDefinitionKey);
        Routes saved = routesRepository.save(route);
        RouteWithBpmn routeWithBpmn = entityDtoMapper.convertRouteEntityToBpmnStruct(saved);
        routeWithBpmn.setBpmnXml(req.bpmnXml());
        return routeWithBpmn;
    }
}
