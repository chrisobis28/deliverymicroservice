package nl.tudelft.sem.template.delivery.services.errors;

import nl.tudelft.sem.template.delivery.external.NotificationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Error;
import org.springframework.stereotype.Component;

@Component
public class SendNotificationAction extends ErrorHandlingAbstractAction {

    private final NotificationService notifications;

    public SendNotificationAction(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Override
    public void handle(Delivery delivery) {

        Error error = delivery.getError();

        switch (error.getType()) {
            case CANCELLED_BY_CLIENT -> {
                notifications.sendNotification(delivery.getCourierID(), "Order you are assigned to deliver was cancelled.");
                notifications.sendNotification(delivery.getRestaurantID(),
                        "Order with delivery id: " + delivery.getDeliveryID() + " was cancelled by the client.");
            }
            case CANCELLED_BY_RESTAURANT -> {
                notifications.sendNotification(delivery.getCustomerID(), "Your order was cancelled by the restaurant.");
            }
            case DELIVERY_UNSUCCESSFUL -> {
                notifications.sendNotification(delivery.getCustomerID(), "The delivery of your order was unsuccessful.");
            }
            case DELIVERY_DELAYED -> {
                notifications.sendNotification(delivery.getCustomerID(),
                        "The delivery of your order was delayed by " + error.getValue() + " minutes.");
            }
        }

        super.handle(delivery);
    }
}
