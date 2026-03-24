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

    boolean existsByBpmnProcessId(String bpmnProcessId);

    @Transactional
    @Modifying
    @Query("update Routes r set r.processDefinitionKey = ?1, r.version = ?2 where r.id = ?3")
    void updateProcessDefinitionKeyAndVersionById(Long processDefinitionKey, Integer version, Long id);

    @Transactional
    @Modifying
    @Query("update Routes r set r.processDefinitionKey = ?1, r.version = ?2, r.active = ?3 where r.id = ?4")
    void updateProcessDefinitionKeyAndVersionAndActiveById(Long processDefinitionKey, Integer version, Boolean active, Long id);
}
