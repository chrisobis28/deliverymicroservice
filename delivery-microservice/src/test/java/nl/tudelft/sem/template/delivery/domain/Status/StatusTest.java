package nl.tudelft.sem.template.delivery.domain.Status;

import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.controllers.DeliveryController;
import nl.tudelft.sem.template.delivery.controllers.RestaurantController;
import nl.tudelft.sem.template.delivery.domain.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusTest {

    private TestDeliveryRepository repo1;

    private DeliveryController sut1;

    private TestRestaurantRepository repo2;

    private RestaurantController sut2;

    public UsersCommunication usersCommunication;

    @BeforeEach
    public void setup() {
        repo1 = new TestDeliveryRepository();
        usersCommunication =  mock(UsersCommunication.class);
        sut1 = new DeliveryController(new DeliveryService(repo1,repo2), usersCommunication);
    }
    @Test
    void get_status_user_unauthorized() {
        Delivery delivery = new Delivery();;
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        String userId = "user@user.com";
        when(usersCommunication.getAccountType(userId)).thenReturn("User");

        delivery.setCourierID(userId);
        sut1.insert(delivery);

        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,userId).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test
    void get_status_courier_unauthorized() {
        //TODO
        Delivery delivery = new Delivery();;
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        delivery.setCourierID("courier@pizza.com");

        String courierID = "user@user.com";
        when(usersCommunication.getAccountType(courierID)).thenReturn("courier");

        sut1.insert(delivery);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody().equals(DeliveryStatus.ACCEPTED));
    }
    @Test
    void get_status_courier_authorized() {
        Delivery delivery = new Delivery();
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        delivery.setCourierID("courier@pizza.com");

        String courierID = "user@user.com";
        when(usersCommunication.getAccountType(courierID)).thenReturn("courier");

        sut1.insert(delivery);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody().equals(DeliveryStatus.ACCEPTED));
    }
    @Test
    void get_status_admin() {
        Delivery delivery = new Delivery();
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        delivery.setCourierID("courier@pizza.com");

        String courierID = "admin@admin.com";
        when(usersCommunication.getAccountType(courierID)).thenReturn("admin");

        sut1.insert(delivery);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody().equals(DeliveryStatus.ACCEPTED));
    }
    @Test
    void update_status_user_unauthorized() {
        Delivery delivery = new Delivery();;
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        String userId = "user@user.com";
        when(usersCommunication.getAccountType(userId)).thenReturn("User");

        delivery.setCourierID(userId);
        sut1.insert(delivery);

        assertThat(sut1.deliveriesDeliveryIdStatusPut(deliveryID,userId,"PREPARING").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test
    void update_status_courier_unauthorized() {
        //TODO
        Delivery delivery = new Delivery();;
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        delivery.setCourierID("courier@pizza.com");

        String courierID = "user@user.com";
        when(usersCommunication.getAccountType(courierID)).thenReturn("courier");

        sut1.insert(delivery);
        assertThat(sut1.deliveriesDeliveryIdStatusPut(deliveryID,courierID,"PREPARING").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody().equals(DeliveryStatus.ACCEPTED));
    }
    @Test
    void update_status_courier_authorized() {
        Delivery delivery = new Delivery();
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        delivery.setCourierID("courier@pizza.com");

        String courierID = "couriern@courier.com";
        when(usersCommunication.getAccountType(courierID)).thenReturn("courier");

        sut1.insert(delivery);
        assertThat(sut1.deliveriesDeliveryIdStatusPut(deliveryID,courierID,"PREPARING").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody().equals(DeliveryStatus.ACCEPTED));
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody().equals(DeliveryStatus.PREPARING));
    }
    @Test
    void update_status_admin() {
        Delivery delivery = new Delivery();
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        delivery.setCourierID("courier@pizza.com");

        String courierID = "admin@admin.com";
        when(usersCommunication.getAccountType(courierID)).thenReturn("admin");

        sut1.insert(delivery);
        assertThat(sut1.deliveriesDeliveryIdStatusPut(deliveryID,courierID,"PREPARING").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody().equals(DeliveryStatus.ACCEPTED));
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody().equals(DeliveryStatus.PREPARING));

    }
}