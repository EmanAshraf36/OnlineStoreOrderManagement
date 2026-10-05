package org.nti.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.nti.model.Customer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CustomerRepository {

    @PersistenceContext
    private EntityManager em;

    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            em.persist(customer);
            return customer;
        }
        return em.merge(customer);
    }

    public Optional<Customer> findById(Long id) {
        return Optional.ofNullable(em.find(Customer.class, id));
    }

    public Optional<Customer> findByEmail(String email) {
        List<Customer> result = em.createQuery(
                        "select c from Customer c where c.email = :email", Customer.class)
                .setParameter("email", email)
                .getResultList();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }

    public List<Customer> findAll() {
        return em.createQuery("select c from Customer c order by c.name", Customer.class)
                .getResultList();
    }
}
