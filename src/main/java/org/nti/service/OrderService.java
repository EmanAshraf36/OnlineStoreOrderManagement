package org.nti.service;

import org.nti.dto.OrderLine;
import org.nti.dto.OrderSummary;
import org.nti.exception.*;
import org.nti.model.*;
import org.nti.repository.CustomerRepository;
import org.nti.repository.OrderRepository;
import org.nti.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        AuditService auditService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Long placeOrder(Long customerId, Map<Long, Integer> productQuantities) {
        try {
            return createOrder(customerId, productQuantities);
        } catch (StoreException e) {
            auditService.record("PLACE_ORDER",
                    "Order failed for customer " + customerId + ": " + e.getMessage(), false);
            throw e;
        }
    }

    private Long createOrder(Long customerId, Map<Long, Integer> productQuantities) {
        Customer customer = findCustomer(customerId);
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new InvalidInputException("An order needs at least one product");
        }

        Order order = new Order(LocalDateTime.now());

        for (Map.Entry<Long, Integer> line : productQuantities.entrySet()) {
            Long productId = line.getKey();
            Integer quantity = line.getValue();

            if (quantity == null || quantity <= 0) {
                throw new InvalidInputException("Quantity for product " + productId + " must be greater than 0");
            }
            Product product = findProduct(productId);
            if (product.getStock() < quantity) {
                throw new InsufficientStockException(product.getSku(), quantity, product.getStock());
            }

            product.setStock(product.getStock() - quantity);
            order.addItem(new OrderItem(product, quantity, product.getPrice()));
        }

        customer.addOrder(order);
        orderRepository.save(order);
        return order.getId();
    }

    @Transactional
    public void pay(Long orderId, PaymentMethod method) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.NEW) {
            throw new InvalidOrderStateException(orderId, order.getStatus(), "pay");
        }
        Payment payment = new Payment(order.getTotal(), method, LocalDateTime.now());
        order.attachPayment(payment);
        order.setStatus(OrderStatus.PAID);
    }

    @Transactional
    public void ship(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new InvalidOrderStateException(orderId, order.getStatus(), "ship");
        }
        order.setStatus(OrderStatus.SHIPPED);
    }

    @Transactional
    public void cancel(Long orderId) {
        Order order = findOrder(orderId);
        OrderStatus status = order.getStatus();
        if (status != OrderStatus.NEW && status != OrderStatus.PAID) {
            throw new InvalidOrderStateException(orderId, status, "cancel");
        }
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public OrderSummary getOrderSummary(Long orderId) {
        Optional<Order> found = orderRepository.findByIdWithItems(orderId);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        Order order = found.get();

        List<OrderLine> lines = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            lines.add(new OrderLine(
                    item.getProduct().getSku(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getLineTotal()));
        }

        PaymentMethod paymentMethod = null;
        if (order.getPayment() != null) {
            paymentMethod = order.getPayment().getMethod();
        }

        return new OrderSummary(order.getId(), order.getCustomer().getName(), order.getStatus(),
                order.getOrderedAt(), lines, order.getTotal(), paymentMethod);
    }

    private Customer findCustomer(Long customerId) {
        Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isEmpty()) {
            throw new ResourceNotFoundException("Customer", customerId);
        }
        return customer.get();
    }

    private Product findProduct(Long productId) {
        Optional<Product> product = productRepository.findById(productId);
        if (product.isEmpty()) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return product.get();
    }

    private Order findOrder(Long orderId) {
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isEmpty()) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        return order.get();
    }
}
