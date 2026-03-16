package com.ike.sb4camunda8.service;

import com.ike.sb4camunda8.dto.DeployReq;
import com.ike.sb4camunda8.dto.RoutesDto;
import com.ike.sb4camunda8.mappers.EntityDtoMapper;
import com.ike.sb4camunda8.repository.RoutesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
@Service
public class RoutesService {
    private final RoutesRepository routesRepository;
    private final EntityDtoMapper entityDtoMapper;

    public RoutesService(RoutesRepository routesRepository, EntityDtoMapper entityDtoMapper) {
        this.routesRepository = routesRepository;
        this.entityDtoMapper = entityDtoMapper;
    }

    @Transactional
    public RoutesDto create(DeployReq req){
        var r = entityDtoMapper.convertDeployReqToRoutesEntity(req);
        var saved = routesRepository.save(r);
        return entityDtoMapper.convertRoutesToRoutesDto(saved);
    }
}
