package org.nti.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.nti.model.Category;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepository {

    @PersistenceContext
    private EntityManager em;

    public Category save(Category category) {
        if (category.getId() == null) {
            em.persist(category);
            return category;
        }
        return em.merge(category);
    }

    public Optional<Category> findByName(String name) {
        List<Category> result = em.createQuery(
                        "select c from Category c where c.name = :name", Category.class)
                .setParameter("name", name)
                .getResultList();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }
}
