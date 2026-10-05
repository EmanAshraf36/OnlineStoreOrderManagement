package org.nti.service;

import org.nti.exception.DuplicateCustomerException;
import org.nti.model.Address;
import org.nti.model.Customer;
import org.nti.repository.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Long register(String name, String email, Address shippingAddress) {
        String normalizedEmail = email.trim().toLowerCase();

        if (customerRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateCustomerException(normalizedEmail);
        }

        Customer customer = new Customer(name, normalizedEmail, shippingAddress);
        try {
            customerRepository.save(customer);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCustomerException(normalizedEmail);
        }
        return customer.getId();
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email.trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }
}
