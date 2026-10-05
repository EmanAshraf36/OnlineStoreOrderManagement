package org.nti.exception;

public class ResourceNotFoundException extends StoreException {

    public ResourceNotFoundException(String type, Long id) {
        super(type + " with id " + id + " not found");
    }
}
