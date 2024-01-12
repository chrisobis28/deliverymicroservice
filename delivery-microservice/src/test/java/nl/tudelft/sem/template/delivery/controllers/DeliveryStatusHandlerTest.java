package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.controllers.DeliveryStatusHandler;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@DataJpaTest
public class DeliveryStatusHandlerTest {

    @Autowired
    private DeliveryRepository deliveryRepository;
    @Mock
    private UsersAuthenticationService usersAuthentication;
    private DeliveryStatusHandler statusHandler;

    @BeforeEach
    public void init() {
        DeliveryService deliveryService = new DeliveryService(deliveryRepository, null);
        statusHandler = new DeliveryStatusHandler(deliveryService, usersAuthentication);
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
    void Returns_delivery_id_when_user_authenticated() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("customer")).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersAuthentication.checkUserAccessToDelivery("customer", delivery)).thenReturn(true);

        assertThat(statusHandler.getDeliveryStatus(delivery.getDeliveryID(), "customer"))
                .extracting("status", "body")
                .containsExactly(HttpStatus.OK, DeliveryStatus.ON_TRANSIT.name());
    }

    @Test
    void Throws_unauthorized_when_user_is_not_known() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("unknown")).thenReturn(AccountType.INVALID);

        assertThatThrownBy(() -> statusHandler.getDeliveryStatus(delivery.getDeliveryID(), "unknown"))
                .extracting("status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void Throws_forbidden_when_user_does_not_have_access_to_delivery() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("other customer")).thenReturn(AccountType.CLIENT);

        assertThatThrownBy(() -> statusHandler.getDeliveryStatus(delivery.getDeliveryID(), "other customer"))
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void Update_succeeds_when_authenticated_user_makes_a_legal_update() {
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
    }

    @Test
    void Throws_forbidden_when_authenticated_user_makes_not_chronological_update() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("restaurant")).thenReturn(AccountType.VENDOR);
        when(usersAuthentication.checkUserAccessToDelivery("restaurant", delivery)).thenReturn(true);

        assertThatThrownBy(() -> statusHandler.updateDeliveryStatus(delivery.getDeliveryID(), "restaurant", "ACCEPTED"))
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void Throws_forbidden_when_authenticated_user_makes_update_not_belonging_to_them() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("client")).thenReturn(AccountType.CLIENT);
        when(usersAuthentication.checkUserAccessToDelivery("client", delivery)).thenReturn(true);

        assertThatThrownBy(() -> statusHandler.updateDeliveryStatus(delivery.getDeliveryID(), "client", "DELIVERED"))
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void Throws_bad_request_when_authenticated_user_makes_update_with_invalid_delivery_status() {
        Delivery delivery = insertExampleDelivery();
        when(usersAuthentication.getUserAccountType("courier")).thenReturn(AccountType.COURIER);
        when(usersAuthentication.checkUserAccessToDelivery("courier", delivery)).thenReturn(true);

        assertThatThrownBy(() -> statusHandler.updateDeliveryStatus(delivery.getDeliveryID(), "courier", "SOMETHING WEIRD"))
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
