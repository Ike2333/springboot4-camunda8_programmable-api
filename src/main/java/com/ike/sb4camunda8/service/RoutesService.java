package com.ike.sb4camunda8.service;

import com.ike.sb4camunda8.dto.CamundaDeployResp;
import com.ike.sb4camunda8.dto.DeployReq;
import com.ike.sb4camunda8.dto.RoutesDto;
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

import javax.management.openmbean.KeyAlreadyExistsException;
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
            throw new KeyAlreadyExistsException("Process ID 已存在: " + processId);
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


    public void updateEnableById(Boolean state, Long id) {
        routesRepository.updateEnableById(state, id);

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
}
