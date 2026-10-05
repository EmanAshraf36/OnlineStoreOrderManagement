package org.nti;

import org.nti.config.JpaConfig;
import org.nti.dto.OrderLine;
import org.nti.dto.OrderSummary;
import org.nti.dto.ProductPage;
import org.nti.exception.DuplicateCustomerException;
import org.nti.exception.InsufficientStockException;
import org.nti.exception.InvalidOrderStateException;
import org.nti.model.Address;
import org.nti.model.PaymentMethod;
import org.nti.service.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JpaConfig.class)) {

            CustomerService customerService = context.getBean(CustomerService.class);
            ProductService productService = context.getBean(ProductService.class);
            OrderService orderService = context.getBean(OrderService.class);
            ReportService reportService = context.getBean(ReportService.class);
            AuditService auditService = context.getBean(AuditService.class);

            section("1. Register customers");
            Long ahmed = customerService.register("Ahmed Ali", "ahmed@mail.com",
                    new Address("12 Tahrir St", "Cairo", "Egypt"));
            Long mona = customerService.register("Mona Hassan", "mona@mail.com",
                    new Address("5 Corniche Rd", "Alexandria", "Egypt"));
            print("Registered Ahmed (id " + ahmed + ") and Mona (id " + mona + ")");
            try {
                customerService.register("Ahmed Again", "AHMED@mail.com",
                        new Address("1 Other St", "Giza", "Egypt"));
            } catch (DuplicateCustomerException e) {
                print("Rejected: " + e.getMessage());
            }

            section("2. Add products");
            Long laptop = productService.addProduct("LAP-001", "Laptop Pro 14", new BigDecimal("45000.00"), 5,
                    "Electronics", "Computers");
            Long mouse = productService.addProduct("MOU-001", "Wireless Mouse", new BigDecimal("650.00"), 50,
                    "Electronics", "Accessories");
            Long keyboard = productService.addProduct("KEY-001", "Mechanical Keyboard", new BigDecimal("2200.00"), 3,
                    "Electronics", "Accessories");
            Long book = productService.addProduct("BOK-001", "Clean Code", new BigDecimal("900.00"), 10,
                    "Books");
            productService.addProduct("LMP-001", "Desk Lamp", new BigDecimal("750.00"), 4,
                    "Home");
            print("Products in the store: " + productService.findPage(0, 10).totalElements());

            section("3. Place orders");
            Long order1 = orderService.placeOrder(ahmed, lines(laptop, 1, mouse, 2));
            print("Order " + order1 + " (Ahmed): 1 x laptop, 2 x mouse");
            Long order2 = orderService.placeOrder(mona, lines(book, 2, keyboard, 1));
            print("Order " + order2 + " (Mona):  2 x book, 1 x keyboard");
            Long order3 = orderService.placeOrder(ahmed, lines(mouse, 1));
            print("Order " + order3 + " (Ahmed): 1 x mouse");

            print("Mouse stock before the failing order: " + productService.getProduct(mouse).getStock());
            try {
                orderService.placeOrder(mona, lines(mouse, 1, keyboard, 10));
            } catch (InsufficientStockException e) {
                print("Rejected: " + e.getMessage());
            }
            print("Mouse stock after the failing order:  " + productService.getProduct(mouse).getStock()
                    + "   <- unchanged: all-or-nothing");

            section("4. Pay, ship, cancel");
            orderService.pay(order1, PaymentMethod.CARD);
            orderService.ship(order1);
            orderService.pay(order2, PaymentMethod.WALLET);
            orderService.cancel(order3);
            print("Order " + order1 + " -> " + orderService.getOrderSummary(order1).status());
            print("Order " + order2 + " -> " + orderService.getOrderSummary(order2).status());
            print("Order " + order3 + " -> " + orderService.getOrderSummary(order3).status());
            print("Mouse stock after cancelling order " + order3 + ": "
                    + productService.getProduct(mouse).getStock() + "   <- 1 mouse back in stock");
            try {
                orderService.pay(order3, PaymentMethod.CASH);
            } catch (InvalidOrderStateException e) {
                print("Rejected: " + e.getMessage());
            }

            section("5. Price change: old orders keep the price they were placed with");
            productService.changePrice(laptop, new BigDecimal("47000.00"));
            print("Laptop price is now " + productService.getProduct(laptop).getPrice());
            printSummary(orderService.getOrderSummary(order1));

            section("6. Product queries (JPQL, Criteria API, paging)");
            print("search(\"lap\", null, null, null):");
            printAll(productService.search("lap", null, null, null));
            print("search(null, 500, 1000, null):");
            printAll(productService.search(null, new BigDecimal("500"), new BigDecimal("1000"), null));
            print("search(\"key\", null, 3000, \"Electronics\"):");
            printAll(productService.search("key", null, new BigDecimal("3000"), "Electronics"));
            print("findByCategory(\"Accessories\"):");
            printAll(productService.findByCategory("Accessories"));
            print("findLowStock(4):");
            printAll(productService.findLowStock(4));
            ProductPage page = productService.findPage(0, 2);
            print("findPage(0, 2): page 1 of " + page.totalPages() + " (" + page.totalElements() + " products)");
            printAll(page.products());

            section("7. Reports");
            print("revenueByCategory():");
            printAll(reportService.revenueByCategory());
            print("topCustomers(5):");
            printAll(reportService.topCustomers(5));
            print("ordersPerStatus(): " + reportService.ordersPerStatus());
            print("productsNeverOrdered():");
            printAll(reportService.productsNeverOrdered());
            int year = LocalDate.now().getYear();
            print("monthlySales(" + year + "):");
            printAll(reportService.monthlySales(year));

            section("8. Bulk discount: 10% off Accessories (one JPQL UPDATE + clear())");
            print("Before: keyboard " + productService.getProduct(keyboard).getPrice()
                    + ", mouse " + productService.getProduct(mouse).getPrice());
            int updated = reportService.applyDiscount("Accessories", 10);
            print("Updated " + updated + " products");
            print("After:  keyboard " + productService.getProduct(keyboard).getPrice()
                    + ", mouse " + productService.getProduct(mouse).getPrice());

            section("9. Audit log (written with REQUIRES_NEW, survived the rollback)");
            printAll(auditService.findAll());

            section("10. Two customers want the LAST watch at the same time");
            Long watch = productService.addProduct("WAT-001", "Limited Edition Watch", new BigDecimal("5000.00"), 1,
                    "Accessories");
            CountDownLatch startSignal = new CountDownLatch(1);
            BuyLastItemTask buyer1 = new BuyLastItemTask(orderService, ahmed, watch, startSignal);
            BuyLastItemTask buyer2 = new BuyLastItemTask(orderService, mona, watch, startSignal);
            Thread thread1 = new Thread(buyer1);
            Thread thread2 = new Thread(buyer2);
            thread1.start();
            thread2.start();
            startSignal.countDown();
            thread1.join();
            thread2.join();
            print("Ahmed: " + buyer1.getResult());
            print("Mona:  " + buyer2.getResult());
            print("Watch stock now: " + productService.getProduct(watch).getStock());
        }
    }

    static class BuyLastItemTask implements Runnable {

        private final OrderService orderService;
        private final Long customerId;
        private final Long productId;
        private final CountDownLatch startSignal;
        private String result = "not run";

        BuyLastItemTask(OrderService orderService, Long customerId, Long productId, CountDownLatch startSignal) {
            this.orderService = orderService;
            this.customerId = customerId;
            this.productId = productId;
            this.startSignal = startSignal;
        }

        @Override
        public void run() {
            try {
                startSignal.await();
                Long orderId = orderService.placeOrder(customerId, lines(productId, 1));
                result = "SUCCESS, order " + orderId;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result = "interrupted";
            } catch (RuntimeException e) {
                result = "FAILED -> " + e.getClass().getSimpleName();
            }
        }

        String getResult() {
            return result;
        }
    }

    static Map<Long, Integer> lines(Long productId, int quantity) {
        Map<Long, Integer> lines = new LinkedHashMap<>();
        lines.put(productId, quantity);
        return lines;
    }

    static Map<Long, Integer> lines(Long product1, int quantity1, Long product2, int quantity2) {
        Map<Long, Integer> lines = new LinkedHashMap<>();
        lines.put(product1, quantity1);
        lines.put(product2, quantity2);
        return lines;
    }

    private static void printSummary(OrderSummary summary) {
        print("Order " + summary.orderId() + " | " + summary.customerName() + " | " + summary.status()
                + " | paid with " + summary.paymentMethod());
        for (OrderLine line : summary.lines()) {
            System.out.printf(">>>   %-8s %-15s x%d  @ %s = %s%n",
                    line.sku(), line.productName(), line.quantity(), line.unitPrice(), line.lineTotal());
        }
        print("  TOTAL " + summary.total());
    }

    private static void printAll(List<?> items) {
        if (items.isEmpty()) {
            print("  (none)");
        }
        for (Object item : items) {
            print("  " + item);
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("==================== " + title + " ====================");
    }

    private static void print(String text) {
        System.out.println(">>> " + text);
    }
}