package org.nti.service;

import org.nti.model.AuditLog;
import org.nti.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String details, boolean success) {
        auditLogRepository.save(new AuditLog(action, details, success));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findAll() {
        return auditLogRepository.findAll();
    }
}
