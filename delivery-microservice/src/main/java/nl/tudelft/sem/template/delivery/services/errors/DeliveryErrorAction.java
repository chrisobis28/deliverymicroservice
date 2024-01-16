package nl.tudelft.sem.template.delivery.services.errors;

import nl.tudelft.sem.template.model.Delivery;

public interface DeliveryErrorAction {
    void setNext(DeliveryErrorAction nextAction);

    void handle(Delivery delivery);
}
