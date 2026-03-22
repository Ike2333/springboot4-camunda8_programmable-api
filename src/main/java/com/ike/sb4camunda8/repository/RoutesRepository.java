package com.ike.sb4camunda8.repository;

import com.ike.sb4camunda8.entity.Routes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
public interface RoutesRepository extends JpaRepository<Routes, Long>, JpaSpecificationExecutor<Routes> {
    List<Routes> findByActive(Boolean active);

    @Transactional
    @Modifying
    @Query("update Routes r set r.active = ?1 where r.id = ?2")
    void updateEnableById(Boolean enable, Long id);

    boolean existsByBpmnProcessId(String bpmnProcessId);

    @Transactional
    @Modifying
    @Query("update Routes r set r.processDefinitionKey = ?1, r.version = ?2 where r.id = ?3")
    void updateProcessDefinitionKeyAndVersionById(Long processDefinitionKey, Integer version, Long id);
}
