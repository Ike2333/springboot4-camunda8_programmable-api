package com.ike.sb4camunda8.service;

import com.ike.sb4camunda8.dto.DeployReq;
import com.ike.sb4camunda8.dto.RoutesDto;
import com.ike.sb4camunda8.entity.Routes;
import com.ike.sb4camunda8.mappers.EntityDtoMapper;
import com.ike.sb4camunda8.repository.RoutesRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.NoSuchElementException;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
@Service
public class RoutesService {

    private final RoutesRepository routesRepository;
    private final EntityDtoMapper entityDtoMapper;
    private final RouteRegisterService routeRegisterService;

    public RoutesService(RoutesRepository routesRepository, EntityDtoMapper entityDtoMapper, RouteRegisterService routeRegisterService) {
        this.routesRepository = routesRepository;
        this.entityDtoMapper = entityDtoMapper;
        this.routeRegisterService = routeRegisterService;
    }

    @Transactional
    public RoutesDto create(DeployReq req) {
        var r = entityDtoMapper.convertDeployReqToRoutesEntity(req);
        var saved = routesRepository.save(r);
        return entityDtoMapper.convertRoutesToRoutesDto(saved);
    }


    public void updateEnableById(Boolean state, Long id){
        routesRepository.updateEnableById(state, id);

    }


    public Page<RoutesDto> findAll(String keyword, Pageable pageable) {
        Specification<Routes> spec = (root, query, cb) -> {
            if (StringUtils.hasText(keyword)) {
                Predicate name = cb.equal(root.get("name"), keyword);
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
