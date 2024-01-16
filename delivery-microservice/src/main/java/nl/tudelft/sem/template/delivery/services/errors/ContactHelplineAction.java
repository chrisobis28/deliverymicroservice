package nl.tudelft.sem.template.delivery.services.errors;

import nl.tudelft.sem.template.delivery.external.CustomerHelplineService;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.stereotype.Component;

@Component
public class ContactHelplineAction extends ErrorHandlingAbstractAction {

    private final CustomerHelplineService helplineService;

    public ContactHelplineAction(CustomerHelplineService helplineService) {
        this.helplineService = helplineService;
    }

    @Override
    public void handle(Delivery delivery) {

        switch (delivery.getError().getType()) {
            case CANCELLED_BY_RESTAURANT -> {
                helplineService.sendRequest(delivery, "The order was cancelled by the restaurant.");
            }
            case DELIVERY_UNSUCCESSFUL -> {
                helplineService.sendRequest(delivery, "The courier did not succeed in delivering the order.");
            }
            case OTHER -> {
                helplineService.sendRequest(delivery, "A non standard error associated with delivery was reported.");
            }
        }

        super.handle(delivery);
    }
}
