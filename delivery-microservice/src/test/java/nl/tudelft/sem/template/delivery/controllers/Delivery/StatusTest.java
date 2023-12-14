package nl.tudelft.sem.template.delivery.controllers.Delivery;

import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.controllers.DeliveryController;
import nl.tudelft.sem.template.delivery.controllers.RestaurantController;
import nl.tudelft.sem.template.delivery.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        repo2 = new TestRestaurantRepository();
        usersCommunication =  mock(UsersCommunication.class);
        sut1 = new DeliveryController(new DeliveryService(repo1,repo2), usersCommunication);
        sut2 = new RestaurantController(new RestaurantService(repo2));
    }
    @Test
    void pickup_get_found() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);

        String restaurantId = "pizzahut@yahoo.com";
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantId);
        restaurant.setLocation(new ArrayList<>(Arrays.asList(100.0, 100.0)));

        delivery.setRestaurantID(restaurantId);
        sut2.insert(restaurant);
        sut1.insert(delivery);


        List<Double> deliveryAddress = sut1.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), null).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        assertThat(sut1.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(),null).getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    void pickup_get_not_found() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        sut1.insert(delivery);
        UUID invalidDeliveryId = UUID.randomUUID();
        assertThat(sut1.deliveriesDeliveryIdPickupLocationGet(invalidDeliveryId,null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    @Test
    void address_get_found() {
        Delivery delivery = new Delivery();;
        UUID deliveryID = UUID.randomUUID();
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setDeliveryID(deliveryID);
        sut1.insert(delivery);
        List<Double> deliveryAddress = sut1.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), null).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        assertThat(sut1.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(),null).getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    void address_get_notFound() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        sut1.insert(delivery);
        UUID invalidDeliveryId = UUID.randomUUID();
        assertThat(sut1.deliveriesDeliveryIdDeliveryAddressGet(invalidDeliveryId,null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody()).isEqualTo("FORBIDDEN");
    }
    @Test
    void get_status_courier_authorized() {
        Delivery delivery = new Delivery();
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        delivery.setCourierID("courier@pizza.com");

        String courierID = "courier@pizza.com";
        when(usersCommunication.getAccountType(courierID)).thenReturn("courier");

        sut1.insert(delivery);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody()).isEqualTo("ACCEPTED");
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
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody()).isEqualTo("ACCEPTED");
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
    void update_status_courier_forbidden() {
        Delivery delivery = new Delivery();;
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        delivery.setCourierID("courier@pizza.com");

        String courierID = "user@user.com";
        when(usersCommunication.getAccountType(courierID)).thenReturn("courier");

        sut1.insert(delivery);
        assertThat(sut1.deliveriesDeliveryIdStatusPut(deliveryID,courierID,"ACCEPTED").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody()).isEqualTo("FORBIDDEN");
        assertThat(sut1.deliveriesDeliveryIdStatusPut(deliveryID,courierID,"PREPARING").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody()).isEqualTo("FORBIDDEN");

    }
    @Test
    void update_status_courier_authorized() {
        Delivery delivery = new Delivery();
        UUID deliveryID = UUID.randomUUID();
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setDeliveryID(deliveryID);
        delivery.setCourierID("courier@pizza.com");

        String courierID = "courier@pizza.com";
        when(usersCommunication.getAccountType(courierID)).thenReturn("courier");


        sut1.insert(delivery);
        assertThat(sut1.deliveriesDeliveryIdStatusPut(deliveryID,courierID,"ACCEPTED").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody()).isEqualTo("ACCEPTED");
        assertThat(sut1.deliveriesDeliveryIdStatusPut(deliveryID,courierID,"PREPARING").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody()).isEqualTo("PREPARING");
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
        assertThat(sut1.deliveriesDeliveryIdStatusPut(deliveryID,courierID,"ACCEPTED").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody()).isEqualTo("ACCEPTED");
        assertThat(sut1.deliveriesDeliveryIdStatusPut(deliveryID,courierID,"PREPARING").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sut1.deliveriesDeliveryIdStatusGet(deliveryID,courierID).getBody()).isEqualTo("PREPARING");

    }
}