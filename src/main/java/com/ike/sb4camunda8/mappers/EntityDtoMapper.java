package com.ike.sb4camunda8.mappers;

import com.ike.sb4camunda8.dto.DeployReq;
import com.ike.sb4camunda8.dto.RoutesDto;
import com.ike.sb4camunda8.entity.Routes;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EntityDtoMapper {
    RoutesDto convertRoutesToRoutesDto(Routes routes);

    List<RoutesDto> convertRoutesListToDtoList(List<Routes> routesList);

    Routes convertRoutesDtoToRoutes(RoutesDto dto);

    @Mapping(source = "sourceName", target = "name")
    Routes convertDeployReqToRoutesEntity(DeployReq req);
}
