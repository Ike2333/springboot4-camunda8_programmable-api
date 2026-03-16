package com.ike.sb4camunda8.repository;

import com.ike.sb4camunda8.entity.Routes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
public interface RoutesRepository extends JpaRepository<Routes, Long> {
    List<Routes> findByEnable(Boolean enable);
}
