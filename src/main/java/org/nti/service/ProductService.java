package org.nti.service;

import org.nti.dto.ProductPage;
import org.nti.exception.DuplicateProductException;
import org.nti.exception.InvalidInputException;
import org.nti.exception.ResourceNotFoundException;
import org.nti.model.Category;
import org.nti.model.Product;
import org.nti.repository.CategoryRepository;
import org.nti.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Long addProduct(String sku, String name, BigDecimal price, int stock, String... categoryNames) {
        if (price == null || price.signum() <= 0) {
            throw new InvalidInputException("Price must be greater than 0");
        }
        if (stock < 0) {
            throw new InvalidInputException("Stock cannot be negative");
        }
        if (productRepository.findBySku(sku).isPresent()) {
            throw new DuplicateProductException(sku);
        }

        Product product = new Product(sku, name, price, stock);
        for (String categoryName : categoryNames) {
            product.addCategory(findOrCreateCategory(categoryName));
        }
        productRepository.save(product);
        return product.getId();
    }

    @Transactional
    public void restock(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new InvalidInputException("Restock quantity must be greater than 0");
        }
        Product product = findProduct(productId);
        product.setStock(product.getStock() + quantity);
    }

    @Transactional
    public void changePrice(Long productId, BigDecimal newPrice) {
        if (newPrice == null || newPrice.signum() <= 0) {
            throw new InvalidInputException("Price must be greater than 0");
        }
        Product product = findProduct(productId);
        product.setPrice(newPrice);
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long productId) {
        return findProduct(productId);
    }

    @Transactional(readOnly = true)
    public List<Product> search(String keyword, BigDecimal minPrice, BigDecimal maxPrice, String category) {
        return productRepository.search(keyword, minPrice, maxPrice, category);
    }

    @Transactional(readOnly = true)
    public List<Product> findByCategory(String categoryName) {
        return productRepository.findByCategory(categoryName);
    }

    @Transactional(readOnly = true)
    public List<Product> findLowStock(int threshold) {
        return productRepository.findLowStock(threshold);
    }

    @Transactional(readOnly = true)
    public ProductPage findPage(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new InvalidInputException("page must be >= 0 and size must be > 0");
        }
        List<Product> products = productRepository.findPage(page, size);
        long total = productRepository.countAll();
        int totalPages = (int) ((total + size - 1) / size);
        return new ProductPage(products, page, size, total, totalPages);
    }

    private Product findProduct(Long productId) {
        Optional<Product> product = productRepository.findById(productId);
        if (product.isEmpty()) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return product.get();
    }

    private Category findOrCreateCategory(String name) {
        Optional<Category> existing = categoryRepository.findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }
        Category category = new Category(name);
        categoryRepository.save(category);
        return category;
    }
}
