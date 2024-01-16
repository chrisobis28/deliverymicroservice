package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.external.CustomerHelplineService;
import nl.tudelft.sem.template.delivery.external.NotificationService;
import nl.tudelft.sem.template.delivery.services.errors.ContactHelplineAction;
import nl.tudelft.sem.template.delivery.services.errors.DeliveryErrorAction;
import nl.tudelft.sem.template.delivery.services.errors.ErrorHandlingChainDefinition;
import nl.tudelft.sem.template.delivery.services.errors.SendNotificationAction;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ErrorHandlingChainTest {

    private final ErrorHandlingChainDefinition factory = new ErrorHandlingChainDefinition();
    @Mock
    private NotificationService notificationService;
    @Mock
    private CustomerHelplineService helplineService;
    @InjectMocks
    private SendNotificationAction sendNotificationAction;
    @InjectMocks
    private ContactHelplineAction contactHelplineAction;
    private DeliveryErrorAction errorHandlingChain;

    @BeforeEach
    void setUp() {
        errorHandlingChain = factory.getDeliveryErrorHandlingChain(sendNotificationAction, contactHelplineAction);
    }

    private Delivery createDeliveryWithErrorType(ErrorType errorType) {

        Error error = new Error().type(errorType);
        Delivery delivery = new Delivery()
                .deliveryID(UUID.randomUUID())
                .error(error)
                .customerID("customer@gmail.com")
                .courierID("courier@gmail.com")
                .restaurantID("restaurant@gmail.com");

        return delivery;
    }

    @Test
    void CANCELLED_BY_CLIENT_error_handling_chain_test() {

        Delivery delivery = createDeliveryWithErrorType(ErrorType.CANCELLED_BY_CLIENT);

        errorHandlingChain.handle(delivery);
        verify(notificationService).sendNotification(eq(delivery.getRestaurantID()), any());
        verify(notificationService).sendNotification(eq(delivery.getCourierID()), any());
    }

    @Test
    void CANCELLED_BY_RESTAURANT_error_handling_chain_test() {

        Delivery delivery = createDeliveryWithErrorType(ErrorType.CANCELLED_BY_RESTAURANT);

        errorHandlingChain.handle(delivery);
        verify(notificationService).sendNotification(eq(delivery.getCustomerID()), any());
        verify(helplineService).sendRequest(eq(delivery), any());
    }

    @Test
    void DELIVERY_UNSUCCESSFUL_error_handling_chain_test() {

        Delivery delivery = createDeliveryWithErrorType(ErrorType.DELIVERY_UNSUCCESSFUL);

        errorHandlingChain.handle(delivery);
        verify(notificationService).sendNotification(eq(delivery.getCustomerID()), any());
        verify(helplineService).sendRequest(eq(delivery), any());
    }

    @Test
    void DELIVERY_DELAYED_error_handling_chain_test() {

        Delivery delivery = createDeliveryWithErrorType(ErrorType.DELIVERY_DELAYED);

        errorHandlingChain.handle(delivery);
        verify(notificationService).sendNotification(eq(delivery.getCustomerID()), any());
    }

    @Test
    void OTHER_error_handling_chain_test() {

        Delivery delivery = createDeliveryWithErrorType(ErrorType.OTHER);

        errorHandlingChain.handle(delivery);
        verify(helplineService).sendRequest(eq(delivery), any());
    }
}
