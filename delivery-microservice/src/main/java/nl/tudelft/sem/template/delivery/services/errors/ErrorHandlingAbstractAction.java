package nl.tudelft.sem.template.delivery.services.errors;

import nl.tudelft.sem.template.model.Delivery;

public abstract class ErrorHandlingAbstractAction implements DeliveryErrorAction {

    private DeliveryErrorAction nextHandler;

    @Override
    public void setNext(DeliveryErrorAction nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public void handle(Delivery delivery) {
        if (nextHandler != null) {
            nextHandler.handle(delivery);
        }
    }

    ;
}
