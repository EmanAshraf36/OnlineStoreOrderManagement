package org.nti.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.nti.model.Order;
import org.nti.model.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    @PersistenceContext
    private EntityManager em;

    public Order save(Order order) {
        if (order.getId() == null) {
            em.persist(order);
            return order;
        }
        return em.merge(order);
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(em.find(Order.class, id));
    }

    public Optional<Order> findByIdWithItems(Long id) {
        List<Order> result = em.createQuery(
                        "select o from Order o " +
                                "join fetch o.customer " +
                                "left join fetch o.items i " +
                                "left join fetch i.product " +
                                "left join fetch o.payment " +
                                "where o.id = :id", Order.class)
                .setParameter("id", id)
                .getResultList();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }

    public List<Order> findByCustomer(Long customerId) {
        return em.createQuery(
                        "select o from Order o where o.customer.id = :customerId " +
                                "order by o.orderedAt desc", Order.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public List<Order> findByStatus(OrderStatus status) {
        return em.createQuery(
                        "select o from Order o where o.status = :status " +
                                "order by o.orderedAt", Order.class)
                .setParameter("status", status)
                .getResultList();
    }
}
