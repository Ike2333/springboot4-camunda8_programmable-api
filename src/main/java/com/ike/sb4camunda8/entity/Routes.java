package com.ike.sb4camunda8.entity;

import com.ike.sb4camunda8.dto.SuppHttpMethod;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 16/3/2026
 */
@Entity
@Table(name = "tb_routes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"method", "path"})
})
public class Routes {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false, comment = "流程名称")
    private String name;

    @Column(nullable = false, comment = "请求方法: post/get/put/delete")
    @Enumerated(EnumType.STRING)
    private SuppHttpMethod method;

    @Column(nullable = false, comment = "请求路径")
    private String path;

    @Column(comment = "Process ID, 从BPMN XML中获取")
    private String bpmnProcessId;

    @Column(unique = true, comment = "由 Camunda 部署时生成, 唯一键, 用于通过 camundaClient 查询")
    private Long processDefinitionKey;

    @Column(comment = "当前流程版本")
    private Integer version;

    @Column(nullable = false, comment = "状态")
    private Boolean active;

    @Column(updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public Routes() {
    }

    public Routes(String name, SuppHttpMethod method, String path, String bpmnProcessId, Long processDefinitionKey, Integer version, Boolean active) {
        this.name = name;
        this.method = method;
        this.path = path;
        this.bpmnProcessId = bpmnProcessId;
        this.processDefinitionKey = processDefinitionKey;
        this.version = version;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SuppHttpMethod getMethod() {
        return method;
    }

    public void setMethod(SuppHttpMethod method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public Long getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(Long processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
