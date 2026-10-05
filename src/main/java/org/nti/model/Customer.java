package org.nti.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    @Embedded
    @AttributeOverride(name = "street", column = @Column(name = "shipping_street", length = 150))
    @AttributeOverride(name = "city", column = @Column(name = "shipping_city", length = 60))
    @AttributeOverride(name = "country", column = @Column(name = "shipping_country", length = 60))
    private Address shippingAddress;

    @OneToMany(mappedBy = "customer")
    private Set<Order> orders = new HashSet<>();

    protected Customer() {
    }

    public Customer(String name, String email, Address shippingAddress) {
        this.name = name;
        this.email = email;
        this.shippingAddress = shippingAddress;
    }

    public void addOrder(Order order) {
        orders.add(order);
        order.setCustomer(this);
    }

    public void removeOrder(Order order) {
        orders.remove(order);
        order.setCustomer(null);
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public Set<Order> getOrders() {
        return orders;
    }

    @Override
    public String toString() {
        return "Customer{id=" + getId() + ", name='" + name + "', email='" + email + "'}";
    }
}