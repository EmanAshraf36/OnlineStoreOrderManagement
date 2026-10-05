package org.nti.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "details", nullable = false, length = 500)
    private String details;

    @Column(name = "success", nullable = false)
    private boolean success;

    protected AuditLog() {
    }

    public AuditLog(String action, String details, boolean success) {
        this.action = action;
        this.details = details;
        this.success = success;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "AuditLog{action='" + action + "', success=" + success + ", details='" + details + "'}";
    }
}
