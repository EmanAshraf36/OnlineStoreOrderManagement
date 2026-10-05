package org.nti.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.nti.model.AuditLog;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuditLogRepository {

    @PersistenceContext
    private EntityManager em;

    public AuditLog save(AuditLog log) {
        em.persist(log);
        return log;
    }

    public List<AuditLog> findAll() {
        return em.createQuery("select a from AuditLog a order by a.id", AuditLog.class)
                .getResultList();
    }
}
