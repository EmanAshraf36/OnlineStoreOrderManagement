package org.nti.exception;

import org.nti.model.OrderStatus;

public class InvalidOrderStateException extends StoreException {

    public InvalidOrderStateException(Long orderId, OrderStatus currentStatus, String action) {
        super("Cannot " + action + " order " + orderId + ": its status is " + currentStatus);
    }
}
