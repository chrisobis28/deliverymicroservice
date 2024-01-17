package nl.tudelft.sem.template.delivery.controllers;

import java.util.UUID;
import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@DataJpaTest
public class DeliveryStatusHandlerTest {

    @Autowired
    private DeliveryRepository deliveryRepository;
    @Mock
    private UsersAuthenticationService usersAuthentication;
    @Mock
    private UsersCommunication usersCommunication;
    private DeliveryStatusHandler statusHandler;

    @BeforeEach
    public void init() {
        DeliveryService deliveryService = new DeliveryService(deliveryRepository, new GPS(), null);
        statusHandler = new DeliveryStatusHandler(deliveryService, usersAuthentication, usersCommunication );
    }

    private Delivery insertExampleDelivery() {
        Delivery delivery = new Delivery()
                .deliveryID(UUID.randomUUID())
                .customerID("customer")
                .courierID("courier")
                .restaurantID("restaurant")
                .status(DeliveryStatus.ON_TRANSIT);
        return deliveryRepository.save(delivery);
    }

    @Test
    void returnsDeliveryIdWhenUserAuthenticated() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("customer")).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersAuthentication.checkUserAccessToDelivery("customer", delivery)).thenReturn(true);

        assertThat(statusHandler.getDeliveryStatus(delivery.getDeliveryID(), "customer"))
                .extracting("status", "body")
                .containsExactly(HttpStatus.OK, DeliveryStatus.ON_TRANSIT.name());
    }

    @Test
    void throwsUnauthorizedWhenUserIsNotKnown() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("unknown")).thenReturn(AccountType.INVALID);

        assertThatThrownBy(() -> statusHandler.getDeliveryStatus(delivery.getDeliveryID(), "unknown"))
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void throwsForbiddenWhenUserDoesNotHaveAccessToDelivery() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("other customer")).thenReturn(AccountType.CLIENT);

        assertThatThrownBy(() -> statusHandler.getDeliveryStatus(delivery.getDeliveryID(), "other customer"))
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateSucceedsWhenAuthenticatedUserMakesALegalUpdate() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("courier")).thenReturn(AccountType.COURIER);
        when(usersAuthentication.checkUserAccessToDelivery("courier", delivery)).thenReturn(true);

        assertThat(statusHandler.updateDeliveryStatus(delivery.getDeliveryID(), "courier", "DELIVERED"))
                .extracting("body.status")
                .isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(deliveryRepository.findById(delivery.getDeliveryID()))
                .get()
                .extracting("status")
                .isEqualTo(DeliveryStatus.DELIVERED);
        verify(usersCommunication, times(1)).updateOrderStatus(any(), any());
    }

    @Test
    void updateSucceedsWhenAuthenticatedUserMakesALegalUpdateAdmin() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("courier")).thenReturn(AccountType.ADMIN);
        when(usersAuthentication.checkUserAccessToDelivery("courier", delivery)).thenReturn(true);

        assertThat(statusHandler.updateDeliveryStatus(delivery.getDeliveryID(), "courier", "DELIVERED"))
                .extracting("body.status")
                .isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(deliveryRepository.findById(delivery.getDeliveryID()))
                .get()
                .extracting("status")
                .isEqualTo(DeliveryStatus.DELIVERED);
        verify(usersCommunication, times(1)).updateOrderStatus(any(), any());
    }

    @Test
    void updateDoesNotSucceedBecauseOtherServerUnavailable() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("courier")).thenReturn(AccountType.COURIER);
        when(usersAuthentication.checkUserAccessToDelivery("courier", delivery)).thenReturn(true);
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(usersCommunication).updateOrderStatus(any(), any());
        assertThatThrownBy(() -> statusHandler.updateDeliveryStatus(delivery.getDeliveryID(), "courier", "DELIVERED"))
                .extracting("status")
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

    }

    @Test
    void throwsForbiddenWhenAuthenticatedUserMakesNotChronologicalUpdate() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("restaurant")).thenReturn(AccountType.VENDOR);
        when(usersAuthentication.checkUserAccessToDelivery("restaurant", delivery)).thenReturn(true);

        assertThatThrownBy(() -> statusHandler.updateDeliveryStatus(delivery.getDeliveryID(), "restaurant", "ACCEPTED"))
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void throwsForbiddenWhenAuthenticatedUserMakesUpdateNotBelongingToThem() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("client")).thenReturn(AccountType.CLIENT);
        when(usersAuthentication.checkUserAccessToDelivery("client", delivery)).thenReturn(true);

        assertThatThrownBy(() -> statusHandler.updateDeliveryStatus(delivery.getDeliveryID(), "client", "DELIVERED"))
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void throwsBadRequestWhenAuthenticatedUserMakesUpdateWithInvalidDeliveryStatus() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("courier")).thenReturn(AccountType.COURIER);
        when(usersAuthentication.checkUserAccessToDelivery("courier", delivery)).thenReturn(true);

        assertThatThrownBy(() -> statusHandler.updateDeliveryStatus(delivery.getDeliveryID(), "courier", "SOMETHING WEIRD"))
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void isStatusUpdateLegalTest() {

    }

}
