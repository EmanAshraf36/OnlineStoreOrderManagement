package org.nti.dto;

import org.nti.model.OrderStatus;
import org.nti.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderSummary(Long orderId,
                           String customerName,
                           OrderStatus status,
                           LocalDateTime orderedAt,
                           List<OrderLine> lines,
                           BigDecimal total,
                           PaymentMethod paymentMethod) {
}