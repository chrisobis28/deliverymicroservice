package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.ErrorService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.delivery.services.errors.DeliveryErrorAction;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@DataJpaTest
class ErrorControllerIntegrationTest {
    @Mock
    private UsersAuthenticationService usersAuth;
    @Mock
    private DeliveryService deliveryService;
    @Mock
    private DeliveryErrorAction errorHandlingChain;

    @Autowired
    private ErrorRepository errorRepository;
    @Autowired
    private DeliveryRepository deliveryRepository;

    private ErrorService errorService;
    private ErrorController errorController;

    private Delivery insertSampleDeliveryAndError() {

        UUID deliveryId = UUID.randomUUID();

        // Entities
        Error error = new Error().errorId(deliveryId).type(ErrorType.DELIVERY_DELAYED);
        Delivery delivery = new Delivery().deliveryID(deliveryId).status(DeliveryStatus.ON_TRANSIT).error(error);

        errorRepository.save(error);
        deliveryRepository.save(delivery);

        return delivery;
    }

    @BeforeEach
    void setUp() {
        errorService = new ErrorService(errorRepository, deliveryRepository);
        errorController = new ErrorController(errorService, deliveryService, usersAuth, errorHandlingChain);
    }

    @Test
    void responds_with_UNAUTHORIZED_when_not_authorized_user_gets_the_error() {

        Delivery delivery = insertSampleDeliveryAndError();
        when(usersAuth.checkUserAccessToDelivery(eq("user@gmail.com"), any())).thenReturn(false);

        assertThatThrownBy(() -> errorController.errorsDeliveryIdGet("user@gmail.com", delivery.getDeliveryID()))
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void responds_with_NOT_FOUND_when_get_is_called_for_error_that_does_not_exist() {

        Delivery delivery = new Delivery().deliveryID(UUID.randomUUID());
        deliveryRepository.save(delivery);

        when(usersAuth.checkUserAccessToDelivery(eq("user@gmail.com"), any())).thenReturn(true);

        assertThatThrownBy(() -> errorController.errorsDeliveryIdGet("user@gmail.com", delivery.getDeliveryID()))
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returns_error_when_error_get_is_called() {

        Delivery delivery = insertSampleDeliveryAndError();
        when(usersAuth.checkUserAccessToDelivery(eq("user@gmail.com"), any())).thenReturn(true);

        assertThat(errorController.errorsDeliveryIdGet("user@gmail.com", delivery.getDeliveryID()).getBody())
                .extracting("errorId", "type")
                .containsExactly(delivery.getDeliveryID(), ErrorType.DELIVERY_DELAYED);
    }

    @Test
    void responds_with_UNAUTHORIZED_when_not_authorized_user_updates_the_error() {

        Delivery delivery = insertSampleDeliveryAndError();
        when(usersAuth.checkUserAccessToDelivery(eq("user@gmail.com"), any())).thenReturn(false);

        assertThatThrownBy(() -> errorController.errorsDeliveryIdPut("user@gmail.com", delivery.getDeliveryID(), null))
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void responds_with_OrderAlreadyDelivered_when_user_updates_error_on_already_delivered_order() {

        Delivery delivery = new Delivery().deliveryID(UUID.randomUUID()).status(DeliveryStatus.DELIVERED);
        deliveryRepository.save(delivery);

        when(usersAuth.checkUserAccessToDelivery(eq("user@gmail.com"), any())).thenReturn(true);

        Error updatedError = new Error().type(ErrorType.DELIVERY_DELAYED);

        assertThatThrownBy(() -> errorController.errorsDeliveryIdPut("user@gmail.com", delivery.getDeliveryID(), updatedError))
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updates_error_when_error_update_is_called() {

        Delivery delivery = insertSampleDeliveryAndError();
        when(usersAuth.checkUserAccessToDelivery(eq("user@gmail.com"), any())).thenReturn(true);

        Error updatedError = new Error().type(ErrorType.CANCELLED_BY_CLIENT);

        assertThat(errorController.errorsDeliveryIdPut("user@gmail.com", delivery.getDeliveryID(), updatedError).getBody())
                .extracting("errorId", "type")
                .containsExactly(delivery.getDeliveryID(), ErrorType.CANCELLED_BY_CLIENT);
        verify(errorHandlingChain, times(1)).handle(any());
    }


}