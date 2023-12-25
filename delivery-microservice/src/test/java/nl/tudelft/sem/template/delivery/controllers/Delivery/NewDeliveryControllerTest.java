package nl.tudelft.sem.template.delivery.controllers.Delivery;

import nl.tudelft.sem.template.delivery.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
//import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.controllers.DeliveryController;
import nl.tudelft.sem.template.delivery.controllers.RestaurantController;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewDeliveryControllerTest {

    private TestDeliveryRepository repo1;

    private DeliveryController sut1;

    private TestRestaurantRepository repo2;

    private RestaurantController sut2;

    public UsersAuthenticationService usersCommunication;

    @BeforeEach
    public void setup() {
        repo1 = new TestDeliveryRepository();
        repo2 = new TestRestaurantRepository();
        usersCommunication =  mock(UsersAuthenticationService.class);
        sut1 = new DeliveryController(new DeliveryService(repo1, repo2), usersCommunication, null);
        sut2 = new RestaurantController(new RestaurantService(repo2));
    }
    @Test
    void pickup_get_found() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerID = "user@user.com";
        delivery.setCustomerID(customerID);
        String restaurantId = "pizzahut@yahoo.com";
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantId);
        restaurant.setLocation(new ArrayList<>(Arrays.asList(100.0, 100.0)));

        delivery.setRestaurantID(restaurantId);
        sut2.insert(restaurant);
        sut1.insert(delivery);

        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        List<Double> deliveryAddress = sut1.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), customerID).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        assertThat(sut1.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(),customerID).getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    void pickup_get_not_found() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        String userId = "user@user.com";
        delivery.setCustomerID(userId);
        sut1.insert(delivery);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        UUID invalidDeliveryId = UUID.randomUUID();
        assertThat(sut1.deliveriesDeliveryIdPickupLocationGet(invalidDeliveryId,userId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    @Test
    void pickup_time_get() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerID = "user@user.com";
        delivery.setCustomerID(customerID);
        String restaurantId = "pizzahut@yahoo.com";
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantId);
        restaurant.setLocation(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setPickupTime(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        delivery.setRestaurantID(restaurantId);
        sut2.insert(restaurant);
        sut1.insert(delivery);

        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        OffsetDateTime pickupTime = sut1.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), customerID).getBody();
        assertThat(pickupTime).isEqualTo(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        assertThat(sut1.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(),customerID).getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    void pickup_time_put() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        String customerID = "user@user.com";
        delivery.setCustomerID(customerID);
        String restaurantId = "pizzahut@yahoo.com";
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantId);
        restaurant.setLocation(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setPickupTime(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        delivery.setRestaurantID(restaurantId);
        sut2.insert(restaurant);
        sut1.insert(delivery);

        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        OffsetDateTime pickupTime;
        pickupTime = sut1.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), customerID).getBody();
        assertThat(pickupTime).isEqualTo(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        assertThat(sut1.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(),customerID).getStatusCode()).isEqualTo(HttpStatus.OK);

        sut1.deliveriesDeliveryIdPickupPut(delivery.getDeliveryID(), customerID,OffsetDateTime.parse("2022-09-30T15:30:00+01:00"));
        pickupTime = sut1.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), customerID).getBody();
        assertThat(pickupTime).isEqualTo(OffsetDateTime.parse("2022-09-30T15:30:00+01:00"));
        assertThat(sut1.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(),customerID).getStatusCode()).isEqualTo(HttpStatus.OK);


    }
    @Test
    void address_get_found() {
        Delivery delivery = new Delivery();
        UUID deliveryID = UUID.randomUUID();
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setDeliveryID(deliveryID);
        String userId = "user@user.com";
        delivery.setCustomerID(userId);
        sut1.insert(delivery);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        List<Double> deliveryAddress = sut1.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), userId).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        assertThat(sut1.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(),userId).getStatusCode()).isEqualTo(HttpStatus.OK);

    }
    @Test
    void address_get_unauthorized() {
        Delivery delivery = new Delivery();
        UUID deliveryID = UUID.randomUUID();
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setDeliveryID(deliveryID);
        String userId = "user@user.com";
        String otherUserId = "newUser@user.com";
        delivery.setCustomerID(otherUserId);
        sut1.insert(delivery);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        List<Double> deliveryAddress = sut1.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), userId).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(List.of()));
        assertThat(sut1.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(),userId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    }
    @Test
    void pickup_get_unauthorized() {
        Delivery delivery = new Delivery();
        UUID deliveryID = UUID.randomUUID();
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setDeliveryID(deliveryID);
        String userId = "user@user.com";
        String otherUserId = "newUser@user.com";
        delivery.setCustomerID(otherUserId);
        sut1.insert(delivery);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        List<Double> deliveryAddress = sut1.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), userId).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(List.of()));
        assertThat(sut1.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(),userId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

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
}