package com.ike.sb4camunda8.mappers;

import com.ike.sb4camunda8.dto.RouteWithBpmn;
import com.ike.sb4camunda8.dto.RoutesDto;
import com.ike.sb4camunda8.entity.Routes;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EntityDtoMapper {
    RoutesDto convertRoutesToRoutesDto(Routes routes);

    RouteWithBpmn convertRouteEntityToBpmnStruct(Routes routes);
}
