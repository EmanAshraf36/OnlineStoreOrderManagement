package org.nti.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.nti.dto.CategoryRevenue;
import org.nti.dto.CustomerSpend;
import org.nti.dto.MonthlySales;
import org.nti.dto.ProductInfo;
import org.nti.model.OrderStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReportRepository {

    private static final List<OrderStatus> SOLD_STATUSES = List.of(OrderStatus.PAID, OrderStatus.SHIPPED);

    @PersistenceContext
    private EntityManager em;

    public List<CategoryRevenue> revenueByCategory() {
        return em.createQuery(
                        "select new org.nti.dto.CategoryRevenue(c.name, sum(i.unitPrice * i.quantity)) " +
                                "from OrderItem i " +
                                "join i.order o " +
                                "join i.product p " +
                                "join p.categories c " +
                                "where o.status in :statuses " +
                                "group by c.name " +
                                "order by sum(i.unitPrice * i.quantity) desc", CategoryRevenue.class)
                .setParameter("statuses", SOLD_STATUSES)
                .getResultList();
    }

    public List<CustomerSpend> topCustomers(int limit) {
        return em.createQuery(
                        "select new org.nti.dto.CustomerSpend(c.name, sum(i.unitPrice * i.quantity)) " +
                                "from Order o " +
                                "join o.customer c " +
                                "join o.items i " +
                                "where o.status in :statuses " +
                                "group by c.id, c.name " +
                                "order by sum(i.unitPrice * i.quantity) desc", CustomerSpend.class)
                .setParameter("statuses", SOLD_STATUSES)
                .setMaxResults(limit)
                .getResultList();
    }

    public Map<OrderStatus, Long> ordersPerStatus() {
        List<Object[]> rows = em.createQuery(
                        "select o.status, count(o) from Order o group by o.status", Object[].class)
                .getResultList();

        Map<OrderStatus, Long> result = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            result.put(status, 0L);
        }
        for (Object[] row : rows) {
            OrderStatus status = (OrderStatus) row[0];
            Long count = (Long) row[1];
            result.put(status, count);
        }
        return result;
    }

    public List<ProductInfo> productsNeverOrdered() {
        return em.createQuery(
                        "select new org.nti.dto.ProductInfo(p.sku, p.name, p.price, p.stock) " +
                                "from Product p " +
                                "where not exists (select i from OrderItem i where i.product = p) " +
                                "order by p.sku", ProductInfo.class)
                .getResultList();
    }

    public List<MonthlySales> monthlySales(int year) {
        LocalDateTime from = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime to = from.plusYears(1);

        return em.createQuery(
                        "select new org.nti.dto.MonthlySales(extract(month from o.orderedAt), " +
                                "                                      sum(i.unitPrice * i.quantity)) " +
                                "from Order o " +
                                "join o.items i " +
                                "where o.status in :statuses " +
                                "and o.orderedAt >= :from and o.orderedAt < :to " +
                                "group by extract(month from o.orderedAt) " +
                                "order by extract(month from o.orderedAt)", MonthlySales.class)
                .setParameter("statuses", SOLD_STATUSES)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public int applyDiscount(String category, double percent) {

        BigDecimal factor = BigDecimal.valueOf(100 - percent).movePointLeft(2);

        em.flush();

        int updated = em.createQuery(
                        "update Product p " +
                                "set p.price = round(p.price * :factor, 2), " +
                                "    p.version = p.version + 1 " +
                                "where p.id in (select p2.id from Product p2 join p2.categories c " +
                                "               where c.name = :category)")
                .setParameter("factor", factor)
                .setParameter("category", category)
                .executeUpdate();

        em.clear();
        return updated;
    }
}
