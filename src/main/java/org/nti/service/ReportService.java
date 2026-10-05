package org.nti.service;
import org.nti.dto.CategoryRevenue;
import org.nti.dto.CustomerSpend;
import org.nti.dto.MonthlySales;
import org.nti.dto.ProductInfo;
import org.nti.exception.InvalidInputException;
import org.nti.model.OrderStatus;
import org.nti.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryRevenue> revenueByCategory() {
        return reportRepository.revenueByCategory();
    }

    @Transactional(readOnly = true)
    public List<CustomerSpend> topCustomers(int limit) {
        if (limit <= 0) {
            throw new InvalidInputException("limit must be greater than 0");
        }
        return reportRepository.topCustomers(limit);
    }

    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> ordersPerStatus() {
        return reportRepository.ordersPerStatus();
    }

    @Transactional(readOnly = true)
    public List<ProductInfo> productsNeverOrdered() {
        return reportRepository.productsNeverOrdered();
    }

    @Transactional(readOnly = true)
    public List<MonthlySales> monthlySales(int year) {
        return reportRepository.monthlySales(year);
    }

    @Transactional
    public int applyDiscount(String category, double percent) {
        if (percent <= 0 || percent >= 100) {
            throw new InvalidInputException("Discount percent must be between 0 and 100 (exclusive)");
        }
        return reportRepository.applyDiscount(category, percent);
    }
}
