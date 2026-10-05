package org.nti.exception;

public class DuplicateCustomerException extends StoreException {

    public DuplicateCustomerException(String email) {
        super("A customer with email " + email + " already exists");
    }
}
