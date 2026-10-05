package org.nti.dto;

import java.math.BigDecimal;

public record OrderLine(String sku, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
}
