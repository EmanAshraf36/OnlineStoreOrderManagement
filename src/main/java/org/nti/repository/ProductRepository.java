package org.nti.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.nti.model.Category;
import org.nti.model.Product;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    @PersistenceContext
    private EntityManager em;

    public Product save(Product product) {
        if (product.getId() == null) {
            em.persist(product);
            return product;
        }
        return em.merge(product);
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(em.find(Product.class, id));
    }

    public Optional<Product> findBySku(String sku) {
        List<Product> result = em.createQuery(
                        "select p from Product p where p.sku = :sku", Product.class)
                .setParameter("sku", sku)
                .getResultList();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }

    public List<Product> findByCategory(String categoryName) {
        return em.createQuery(
                        "select p from Product p join p.categories c " +
                                "where c.name = :categoryName " +
                                "order by p.name", Product.class)
                .setParameter("categoryName", categoryName)
                .getResultList();
    }

    public List<Product> search(String keyword, BigDecimal minPrice, BigDecimal maxPrice, String category) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> product = cq.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            Predicate inName = cb.like(cb.lower(product.<String>get("name")), pattern);
            Predicate inSku = cb.like(cb.lower(product.<String>get("sku")), pattern);
            predicates.add(cb.or(inName, inSku));
        }
        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(product.<BigDecimal>get("price"), minPrice));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(product.<BigDecimal>get("price"), maxPrice));
        }
        if (category != null && !category.isBlank()) {
            Join<Product, Category> categoryJoin = product.join("categories");
            predicates.add(cb.equal(cb.lower(categoryJoin.<String>get("name")), category.trim().toLowerCase()));
        }

        cq.select(product)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.asc(product.get("name")));

        return em.createQuery(cq).getResultList();
    }

    public List<Product> findLowStock(int threshold) {
        return em.createQuery(
                        "select p from Product p where p.stock <= :threshold " +
                                "order by p.stock, p.sku", Product.class)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    public List<Product> findPage(int page, int size) {
        return em.createQuery("select p from Product p order by p.id", Product.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long countAll() {
        return em.createQuery("select count(p) from Product p", Long.class)
                .getSingleResult();
    }
}
