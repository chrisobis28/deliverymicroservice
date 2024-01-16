package nl.tudelft.sem.template.delivery.external;

import nl.tudelft.sem.template.model.Delivery;


public interface CustomerHelplineService {

    void sendRequest(Delivery delivery, String message);
}
