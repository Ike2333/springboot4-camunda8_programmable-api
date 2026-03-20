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
public class Routes {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SuppHttpMethod method;

    @Column(nullable = false)
    private String path;

    // 使用byte[]性能会更好, 此处为了可读性使用String
    @Lob
    private String bpmnXml;

    @Column(unique = true, nullable = false)
    private String uniqMethodPath;

    @Column(nullable = false)
    private Boolean enable;

    @Column(updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void updateUniqMethodPath() {
        if (this.method != null && this.path != null) {
            this.uniqMethodPath = this.method.name() + ":" + this.path;
        }
    }


    public void setMethod(SuppHttpMethod method) {
        this.method = method;
        updateUniqMethodPath();
    }

    public void setPath(String path) {
        this.path = path;
        updateUniqMethodPath();
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBpmnXml(String bpmnXml) {
        this.bpmnXml = bpmnXml;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SuppHttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getBpmnXml() {
        return bpmnXml;
    }

    public String getUniqMethodPath() {
        return uniqMethodPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Boolean getEnable() {
        return enable;
    }
}
