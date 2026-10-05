package org.nti.exception;

public class DuplicateProductException extends StoreException {

    public DuplicateProductException(String sku) {
        super("A product with SKU " + sku + " already exists");
    }
}
