package nl.tudelft.sem.template.delivery.controllers.Delivery;

import nl.tudelft.sem.template.delivery.AddressAdapter;
import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
//import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.controllers.DeliveryController;
import nl.tudelft.sem.template.delivery.controllers.RestaurantController;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        sut2 = new RestaurantController(new RestaurantService(repo2), new AddressAdapter(new GPS()));
    }

    @Test
    void getUnexpectedEventBadRequest() {
        UUID del_id = UUID.randomUUID();
        ResponseEntity<Error> res = sut1.deliveriesDeliveryIdUnexpectedEventGet(del_id, null);
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
        res = sut1.deliveriesDeliveryIdUnexpectedEventGet(del_id, "");
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getUnexpectedEventNotFound() {
        UUID del_id = UUID.randomUUID();
        String courierID = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, courierID));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCourierID(courierID);

        sut1.insert(delivery);
        ResponseEntity<Error> res = sut1.deliveriesDeliveryIdUnexpectedEventGet(del_id, courierID);
        assertEquals(res.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void getUnexpectedEventUnauthorized() {
        UUID del_id = UUID.randomUUID();
        Error e = new Error();
        e.setType(ErrorType.NONE);
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_user@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setError(e);
        delivery.setCustomerID(cID + "h");
        delivery.setCourierID(coID + "h");
        delivery.setRestaurantID(vID + "h");

        sut1.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(false);

        ResponseEntity<Error> res = sut1.deliveriesDeliveryIdUnexpectedEventGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut1.deliveriesDeliveryIdUnexpectedEventGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut1.deliveriesDeliveryIdUnexpectedEventGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);
    }

    @Test
    void getUnexpectedEventAuthorized() {
        String admin = "hi_im_an_admin@gmail.com";
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_vendor@gmail.com";
        Error e = new Error();
        e.setType(ErrorType.NONE);
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID);
        delivery.setCourierID(coID);
        delivery.setRestaurantID(vID);
        delivery.setError(e);

        sut1.insert(delivery);

        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(admin, delivery)).thenReturn(true);

        ResponseEntity<Error> res = sut1.deliveriesDeliveryIdUnexpectedEventGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody().getType(), ErrorType.NONE);

        res = sut1.deliveriesDeliveryIdUnexpectedEventGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody().getType(), ErrorType.NONE);

        res = sut1.deliveriesDeliveryIdUnexpectedEventGet(del_id, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody().getType(), ErrorType.NONE);

        res = sut1.deliveriesDeliveryIdUnexpectedEventGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody().getType(), ErrorType.NONE);
    }

    @Test
    void getRestaurantBadRequest() {
        UUID del_id = UUID.randomUUID();
        ResponseEntity<String> res = sut1.deliveriesDeliveryIdRestaurantGet(del_id, null);
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
        res = sut1.deliveriesDeliveryIdRestaurantGet(del_id, "");
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getRestaurantNotFound() {
        UUID del_id = UUID.randomUUID();
        String courierID = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, courierID));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCourierID(courierID);

        sut1.insert(delivery);
        ResponseEntity<String> res = sut1.deliveriesDeliveryIdRestaurantGet(del_id, courierID);
        assertEquals(res.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void getRestaurantUnauthorized() {
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_user@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID + "h");
        delivery.setCourierID(coID + "h");
        delivery.setRestaurantID(vID + "h");

        sut1.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(false);

        ResponseEntity<String> res = sut1.deliveriesDeliveryIdRestaurantGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut1.deliveriesDeliveryIdRestaurantGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut1.deliveriesDeliveryIdRestaurantGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);
    }

    @Test
    void getRestaurantAuthorized() {
        String admin = "hi_im_an_admin@gmail.com";
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_vendor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID);
        delivery.setCourierID(coID);
        delivery.setRestaurantID(vID);

        sut1.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(admin)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(admin, delivery)).thenReturn(true);

        ResponseEntity<String> res = sut1.deliveriesDeliveryIdRestaurantGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), vID);

        res = sut1.deliveriesDeliveryIdRestaurantGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), vID);

        res = sut1.deliveriesDeliveryIdRestaurantGet(del_id, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), vID);

        res = sut1.deliveriesDeliveryIdRestaurantGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);
    }

    @Test
    void getDeliveredTimeBadRequest() {
        UUID del_id = UUID.randomUUID();
        ResponseEntity<OffsetDateTime> res = sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, null);
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
        res = sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, "");
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getDeliveredTimeNotFound() {
        UUID del_id = UUID.randomUUID();
        String customerID = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, customerID));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(customerID);

        sut1.insert(delivery);
        ResponseEntity<OffsetDateTime> res = sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, customerID);
        assertEquals(res.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void getDeliveredTimeUnauthorized() {
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_user@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID + "h");
        delivery.setCourierID(coID + "h");
        delivery.setRestaurantID(vID + "h");

        sut1.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(false);

        ResponseEntity<String> res = sut1.deliveriesDeliveryIdCustomerGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut1.deliveriesDeliveryIdCustomerGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut1.deliveriesDeliveryIdCustomerGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);
    }

    @Test
    void getDeliveredTimeAuthorized() {
        String admin = "hi_im_an_admin@gmail.com";
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_vendor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID);
        delivery.setCourierID(coID);
        delivery.setRestaurantID(vID);
        OffsetDateTime t = OffsetDateTime.of(2023, 12, 27, 12, 24, 10, 4, ZoneOffset.ofHours(0));
        delivery.setDeliveredTime(t);

        sut1.insert(delivery);

        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(admin, delivery)).thenReturn(true);

        ResponseEntity<OffsetDateTime> res = sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), t);

        res = sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), t);

        res = sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), t);

        res = sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void getCustomerBadRequest() {
        UUID del_id = UUID.randomUUID();
        ResponseEntity<String> res = sut1.deliveriesDeliveryIdCustomerGet(del_id, null);
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
        res = sut1.deliveriesDeliveryIdCustomerGet(del_id, "");
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getCustomerDeliveryNotFound() {
        UUID del_id = UUID.randomUUID();
        String customerID = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut1.deliveriesDeliveryIdDeliveredTimeGet(del_id, customerID));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(customerID);

        sut1.insert(delivery);
        ResponseEntity<String> res = sut1.deliveriesDeliveryIdCustomerGet(del_id, customerID);
        assertEquals(res.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void getCustomerDeliveryUnauthorized() {
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_user@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID + "h");
        delivery.setCourierID(coID + "h");
        delivery.setRestaurantID(vID + "h");

        sut1.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(false);

        ResponseEntity<String> res = sut1.deliveriesDeliveryIdCustomerGet(del_id, cID);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());

        res = sut1.deliveriesDeliveryIdCustomerGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut1.deliveriesDeliveryIdCustomerGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);
    }

    @Test
    void getCustomerDeliveryAuthorized() {
        String admin = "hi_im_an_admin@gmail.com";
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_vendor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID);
        delivery.setCourierID(coID);
        delivery.setRestaurantID(vID);

        sut1.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(admin)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(admin, delivery)).thenReturn(true);

        ResponseEntity<String> res = sut1.deliveriesDeliveryIdCustomerGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), cID);

        res = sut1.deliveriesDeliveryIdCustomerGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), cID);

        res = sut1.deliveriesDeliveryIdCustomerGet(del_id, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), cID);

        res = sut1.deliveriesDeliveryIdCustomerGet(del_id, cID);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
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