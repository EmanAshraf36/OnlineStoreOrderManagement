package org.nti.dto;

import org.nti.model.Product;

import java.util.List;

public record ProductPage(List<Product> products, int page, int size, long totalElements, int totalPages) {
}
