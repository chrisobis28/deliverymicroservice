package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import nl.tudelft.sem.template.model.RestaurantsPostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
class RestaurantControllerTest {

    @Autowired
    private DeliveryRepository repo1;
    @Autowired
    private RestaurantRepository repo2;
    private RestaurantController sut;
    private RestaurantService rs;
    private UsersAuthenticationService usersCommunication;
    List<Double> coord;

    @BeforeEach
    void setUp() {
        // Mock data
        coord = List.of(32.6, 50.4);

        usersCommunication = mock(UsersAuthenticationService.class);
        rs = new RestaurantService(repo2, repo1);
        sut = new RestaurantController(rs, usersCommunication);
    }

    @Test
    void restaurantsPostNullRpr() {
        assertThatThrownBy(() -> sut.restaurantsPost(null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.restaurantsPost(null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"Restaurant could not be created.\"");
    }

    @Test
    public void isValidAddressTest() {
        assertTrue(sut.isInvalidAddress(List.of(0.0,0.0,0.0)));
    }
    @Test
    public void isValidAddressTest1() {
        assertFalse(sut.isInvalidAddress(List.of(0.0,0.0)));
    }

    @Test
    public void isValidAddressTest2() {
        assertTrue(sut.isInvalidAddress(null));
    }

    @Test
    public void isNullOrEmptyTest() {
        assertTrue(sut.isNullOrEmpty(null));
    }
    @Test
    public void isNullOrEmptyTest1() {
        assertTrue(sut.isNullOrEmpty(""));
    }

    @Test
    public void isNullOrEmptyTest2() {
        assertTrue(sut.isNullOrEmpty(" "));
    }

    @Test
    public void isNullOrEmptyTest3() {
        assertFalse(sut.isNullOrEmpty("bjefef "));
    }

    @Test
    void restaurantsInvalidAddr() {
        RestaurantsPostRequest rpr = new RestaurantsPostRequest();
        rpr.setRestaurantID("hi_im_a_vendor@testmail.com");
        rpr.setLocation(List.of());

        assertThatThrownBy(() -> sut.restaurantsPost(rpr))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.restaurantsPost(rpr))
            .message()
            .isEqualTo("400 BAD_REQUEST \"Restaurant ID or location is invalid.\"");
    }

    @Test
    void restaurantsInvalidEmail() {
        RestaurantsPostRequest rpr = new RestaurantsPostRequest();
        rpr.setRestaurantID(null);
        rpr.setLocation(coord);

        assertThatThrownBy(() -> sut.restaurantsPost(rpr))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.restaurantsPost(rpr))
            .message()
            .isEqualTo("400 BAD_REQUEST \"Restaurant ID or location is invalid.\"");
    }

    @Test
    void restaurantsEmptyEmail() {
        RestaurantsPostRequest rpr = new RestaurantsPostRequest();
        rpr.setRestaurantID("");
        rpr.setLocation(coord);

        assertThatThrownBy(() -> sut.restaurantsPost(rpr))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.restaurantsPost(rpr))
            .message()
            .isEqualTo("400 BAD_REQUEST \"Restaurant ID or location is invalid.\"");
    }

    @Test
    void restaurantsInvalidAddr2() {
        RestaurantsPostRequest rpr = new RestaurantsPostRequest();
        rpr.setRestaurantID("hi_im_a_vendor@testmail.com");
        rpr.setLocation(List.of(0.0, 0.0, 0.0));

        assertThatThrownBy(() -> sut.restaurantsPost(rpr))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.restaurantsPost(rpr))
            .message()
            .isEqualTo("400 BAD_REQUEST \"Restaurant ID or location is invalid.\"");
    }

    @Test
    void restaurantsPost() {
        RestaurantsPostRequest rpr = new RestaurantsPostRequest();
        rpr.setRestaurantID("hi_im_a_vendor@testmail.com");
        rpr.setLocation(coord);
        //when(sut.mockGPS.getCoordinatesOfAddress(addr)).thenReturn(co_ord);
        Restaurant r = new Restaurant();
        r.setRestaurantID("hi_im_a_vendor@testmail.com");
        r.setLocation(coord);
        //sut.insert(r);

        ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);
        Restaurant added = result.getBody();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(added.getLocation().get(0), r.getLocation().get(0));
        assertEquals(added.getLocation().get(1), r.getLocation().get(1));
    }

    @Test
    void restaurantsRestaurantIdLocationPutAdmin() {
        String restaurantId = "restaurant_admin@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        List<Double> list = new ArrayList<>();
        list.add(0.0);
        list.add(0.0);
        List<Double> list2 = new ArrayList<>();
        list2.add(0.1);
        list2.add(0.1);
        r.setLocation(list);
        sut.insert(r);

        String userId = "user_admin@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);


        ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, list2);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(res.getBody().getLocation(), List.of(0.1, 0.1));
    }

    @Test
    void restaurantsRestaurantIdLocationPutSameVendor() {
        String restaurantId = "restaurant_sameVendor@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        List<Double> list = new ArrayList<>();
        list.add(0.0);
        list.add(0.0);
        List<Double> list2 = new ArrayList<>();
        list2.add(0.1);
        list2.add(0.1);
        r.setLocation(list);
        rs.insert(r);

        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);

        ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdLocationPut(restaurantId, restaurantId, list2);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(res.getBody().getLocation(), List.of(0.1, 0.1));
    }

    @Test
    void restaurantsRestaurantIdLocationPutDiffVendor() {
        String restaurantId = "restaurant_diffVendor@testmail.com";
        String otherRestaurantId = "other_restaurant_diffVendor@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(List.of(0.0, 0.0));
        sut.insert(r);

        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(otherRestaurantId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, otherRestaurantId, List.of(0.1, 0.1)));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "User lacks necessary permissions.");
    }

    @Test
    void restaurantsRestaurantIdLocationPutCourier() {
        String restaurantId = "restaurant_courier@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(List.of(0.0, 0.0));
        sut.insert(r);

        String userId = "user_courier@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "Only vendors and admins can change the restaurant's address");
    }

    @Test
    void restaurantsRestaurantIdLocationPutClient() {
        String restaurantId = "restaurant_client@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(List.of(0.0, 0.0));
        sut.insert(r);

        String userId = "user_client@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "Only vendors and admins can change the restaurant's address");
    }

    @Test
    void restaurantsRestaurantIdLocationPutInvalid() {
        String restaurantId = "restaurant_invalid@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(List.of(0.0, 0.0));
        sut.insert(r);

        String userId = "user_invalid@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals(exception.getReason(), "User lacks valid authentication credentials.");
    }

    @Test
    void restaurantsRestaurantIdLocationPutNotFound() {
        String restaurantId = "restaurant_not_found@testmail.com";

        String userId = "user_not_found@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
        assertEquals(exception.getReason(), "Restaurant with specified id not found");
    }

    @Test
    void restaurantsRestaurantIdDeliverZonePutAdmin() {
        String restaurantId = "restaurant_admin@testmail.com";
        Restaurant r = new Restaurant();
        r.setLocation(List.of(6.7, 6.7));
        r.setRestaurantID(restaurantId);
        r.setDeliveryZone(10.0);
        sut.insert(r);

        String userId = "user_admin@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(res.getBody().getDeliveryZone(), 20.0);
    }

    @Test
    void restaurantsRestaurantIdDeliverZonePutEmptyCourierList() {
        String restaurantId = "restaurant_sameVendor@testmail.com";
        String otherRestaurantId = "other_restaurant_diffVendor@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setDeliveryZone(10.0);
        r.setLocation(coord);
        r.setCouriers(List.of());
        sut.insert(r);

        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        //when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(otherRestaurantId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, otherRestaurantId, 20.0));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "User lacks necessary permissions.");
    }

    @Test
    void restaurantsRestaurantIdDeliverZonePutSameVendor() {
        String restaurantId = "restaurant_sameVendor@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(List.of(1.2, 3.4));
        r.setDeliveryZone(10.0);
        r.setCouriers(List.of("example_courier@testmail.com"));
        sut.insert(r);

        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);

        ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, restaurantId, 20.0);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(res.getBody().getDeliveryZone(), 20.0);
    }

    @Test
    void restaurantsRestaurantIdDeliverZonePutSameVendorNoCouriers() {
        String restaurantId = "restaurant_sameVendor@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(List.of(1.2, 3.4));
        r.setDeliveryZone(10.0);
        r.setCouriers(new ArrayList<>());
        sut.insert(r);

        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, restaurantId, 20.0));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "User lacks necessary permissions.");
    }

    @Test
    void restaurantsRestaurantIdDeliverZonePutDiffVendor() {
        String restaurantId = "restaurant_diffVendor@testmail.com";
        String otherRestaurantId = "other_restaurant_diffVendor@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(coord);
        r.setDeliveryZone(10.0);
        sut.insert(r);

        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(otherRestaurantId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, otherRestaurantId, 20.0));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "User lacks necessary permissions.");
    }



    @Test
    void restaurantsRestaurantIdDeliverZonePutCourier() {
        String restaurantId = "restaurant_courier@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setDeliveryZone(10.0);
        assertThatThrownBy(() -> sut.insert(r))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.insert(r))
            .message()
            .isEqualTo("400 BAD_REQUEST \"Restaurant is invalid.\"");

        String userId = "user_courier@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "Only vendors and admins can change the delivery zone of a restaurant.");
    }

    @Test
    void restaurantsRestaurantIdDeliverZonePutClient() {
        String restaurantId = "restaurant_client@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(List.of(0.1, 0.2));
        r.setDeliveryZone(10.0);
        ResponseEntity<Void> result = sut.insert(r);
        assertEquals(HttpStatus.OK, result.getStatusCode());

        String userId = "user_client@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "Only vendors and admins can change the delivery zone of a restaurant.");
    }

    @Test
    void restaurantsRestaurantIdDeliverZonePutInvalid() {
        String restaurantId = "restaurant_invalid@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(coord);
        r.setDeliveryZone(10.0);
        sut.insert(r);

        String userId = "user_invalid@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals(exception.getReason(), "User lacks valid authentication credentials.");
    }

    @Test
    void restaurantsRestaurantIdDeliverZonePutNotFound() {
        String restaurantId = "restaurant_not_found@testmail.com";

        String userId = "user_not_found@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
        assertEquals(exception.getReason(), "Restaurant with specified id not found");
    }

    @Test
    void restaurantsRestaurantIdNewOrdersGetAdmin() {
        String restaurantId = "restaurant_neworders_admin@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setDeliveryZone(10.0);
        r.setLocation(List.of(6.7, 6.7));
        sut.insert(r);
        // FIRST DELIVERY ON TRANSIT
        UUID deliveryId1 = UUID.randomUUID();
        Delivery d1 = new Delivery();
        d1.setDeliveryID(deliveryId1);
        d1.setRestaurantID(restaurantId);
        d1.setStatus(DeliveryStatus.ON_TRANSIT);

        assertThatThrownBy(() -> sut.insert((Delivery) null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.insert((Delivery) null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"Delivery is invalid.\"");

        // SECOND DELIVERY ACCEPTED
        UUID deliveryId2 = UUID.randomUUID();
        Delivery d2 = new Delivery();
        d2.setDeliveryID(deliveryId2);
        d2.setRestaurantID(restaurantId);
        d2.setStatus(DeliveryStatus.ACCEPTED);
        d2.setCourierID(null);
        sut.insert(d2);

        // THIRD DELIVERY PREPARING BUT CourierID NOT NULL
        UUID deliveryId3 = UUID.randomUUID();
        Delivery d3 = new Delivery();
        d3.setDeliveryID(deliveryId3);
        d3.setRestaurantID(restaurantId);
        d3.setStatus(DeliveryStatus.PREPARING);
        d3.setCourierID("courier@testmail.com");
        sut.insert(d3);

        String userId = "user_admin@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<List<Delivery>> res = sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, userId);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(res.getBody().get(0).getDeliveryID(), deliveryId2);
        assertEquals(res.getBody().size(), 1);
    }

    @Test
    void restaurantsRestaurantIdNewOrdersGetSameVendor() {
        String restaurantId = "restaurant_neworders_same_vendor@testmail.com";
        Restaurant r = new Restaurant();
        r.setLocation(List.of(5.6, 67.7));
        r.setRestaurantID(restaurantId);
        sut.insert(r);

        // FIRST DELIVERY ON TRANSIT
        UUID deliveryId1 = UUID.randomUUID();
        Delivery d1 = new Delivery();
        d1.setDeliveryID(deliveryId1);
        d1.setRestaurantID(restaurantId);
        d1.setStatus(DeliveryStatus.ON_TRANSIT);
        sut.insert(d1);

        // SECOND DELIVERY ACCEPTED
        UUID deliveryId2 = UUID.randomUUID();
        Delivery d2 = new Delivery();
        d2.setDeliveryID(deliveryId2);
        d2.setRestaurantID(restaurantId);
        d2.setStatus(DeliveryStatus.ACCEPTED);
        d2.setCourierID(null);
        sut.insert(d2);

        // THIRD DELIVERY PREPARING BUT CourierID NOT NULL
        UUID deliveryId3 = UUID.randomUUID();
        Delivery d3 = new Delivery();
        d3.setDeliveryID(deliveryId3);
        d3.setRestaurantID(restaurantId);
        d3.setStatus(DeliveryStatus.PREPARING);
        d3.setCourierID("courier@testmail.com");
        sut.insert(d3);

        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);

        ResponseEntity<List<Delivery>> res = sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, restaurantId);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(res.getBody().get(0).getDeliveryID(), deliveryId2);
        assertEquals(res.getBody().size(), 1);
    }

    @Test
    void restaurantsRestaurantIdNewOrdersGetDiffVendor() {
        String restaurantId = "restaurant_neworders_diff_vendor@testmail.com";
        String other_restaurant = "restaurant_neworders_diff_vendor_other@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(coord);
        r.setDeliveryZone(10.0);
        sut.insert(r);

        // FIRST DELIVERY ON TRANSIT
        UUID deliveryId1 = UUID.randomUUID();
        Delivery d1 = new Delivery();
        d1.setDeliveryID(deliveryId1);
        d1.setRestaurantID(restaurantId);
        d1.setStatus(DeliveryStatus.ON_TRANSIT);
        sut.insert(d1);

        // SECOND DELIVERY ACCEPTED
        UUID deliveryId2 = UUID.randomUUID();
        Delivery d2 = new Delivery();
        d2.setDeliveryID(deliveryId2);
        d2.setRestaurantID(restaurantId);
        d2.setStatus(DeliveryStatus.ACCEPTED);
        d2.setCourierID(null);
        sut.insert(d2);

        // THIRD DELIVERY PREPARING BUT CourierID NOT NULL
        UUID deliveryId3 = UUID.randomUUID();
        Delivery d3 = new Delivery();
        d3.setDeliveryID(deliveryId3);
        d3.setRestaurantID(restaurantId);
        d3.setStatus(DeliveryStatus.PREPARING);
        d3.setCourierID("courier@testmail.com");
        sut.insert(d3);

        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(other_restaurant)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, other_restaurant));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "User lacks necessary permissions.");
    }

    @Test
    void restaurantsRestaurantIdNewOrdersGetClient() {
        String restaurantId = "restaurant_neworders_client@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(coord);
        r.setDeliveryZone(10.0);
        sut.insert(r);
        // FIRST DELIVERY ACCEPTED
        UUID deliveryId1 = UUID.randomUUID();
        Delivery d1 = new Delivery();
        d1.setDeliveryID(deliveryId1);
        d1.setRestaurantID(restaurantId);
        d1.setStatus(DeliveryStatus.ACCEPTED);
        sut.insert(d1);

        String userId = "user_admin@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, userId));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "User lacks necessary permissions.");
    }

    @Test
    void restaurantsRestaurantIdNewOrdersGetCourier() {
        String restaurantId = "restaurant_neworders_courier@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(coord);
        r.setDeliveryZone(10.0);
        sut.insert(r);
        // FIRST DELIVERY ACCEPTED
        UUID deliveryId1 = UUID.randomUUID();
        Delivery d1 = new Delivery();
        d1.setDeliveryID(deliveryId1);
        d1.setRestaurantID(restaurantId);
        d1.setStatus(DeliveryStatus.ACCEPTED);
        sut.insert(d1);

        String userId = "user_admin@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, userId));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "User lacks necessary permissions.");
    }

    @Test
    void restaurantsRestaurantIdNewOrdersGetNotFound() {
        String restaurantId = "restaurant_neworders_not_found@testmail.com";
        // FIRST DELIVERY ACCEPTED
        UUID deliveryId1 = UUID.randomUUID();
        Delivery d1 = new Delivery();
        d1.setDeliveryID(deliveryId1);
        d1.setRestaurantID(restaurantId);
        d1.setStatus(DeliveryStatus.ACCEPTED);
        sut.insert(d1);

        String userId = "user_admin@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(RestaurantService.RestaurantNotFoundException.class,
                () -> sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, userId));
        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
        assertEquals(exception.getReason(), "Restaurant with specified id not found");
    }

    @Test
    void restaurantsRestaurantIdNewOrdersGetInvalid() {
        String restaurantId = "restaurant_neworders_courier@testmail.com";
        Restaurant r = new Restaurant();
        r.setRestaurantID(restaurantId);
        r.setLocation(coord);
        r.setDeliveryZone(10.0);
        sut.insert(r);
        // FIRST DELIVERY ACCEPTED
        UUID deliveryId1 = UUID.randomUUID();
        Delivery d1 = new Delivery();
        d1.setDeliveryID(deliveryId1);
        d1.setRestaurantID(restaurantId);
        d1.setStatus(DeliveryStatus.ACCEPTED);
        sut.insert(d1);

        String userId = "user_admin@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, userId));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals(exception.getReason(), "User lacks valid authentication credentials.");
    }

    @Test
    void restaurantDeleteFound() {
        RestaurantsPostRequest rpr = new RestaurantsPostRequest();
        rpr.setRestaurantID("hi_im_a_vendor@testmail.com");
        rpr.setLocation(coord);

        sut.restaurantsPost(rpr);
        ResponseEntity<String> result = sut.restaurantsRestaurantIdDelete("hi_im_a_vendor@testmail.com");
        assertThrows(RestaurantService.RestaurantNotFoundException.class,
                () -> sut.restaurantsRestaurantIdDelete("hi_im_a_vendor@testmail.com"));
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void restaurantsRestaurantIdGetNull() {
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdGet("bla", null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdGet("bla", null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID or Restaurant ID is invalid.\"");
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdGet(null, "bla"))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdGet(null, "bla"))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID or Restaurant ID is invalid.\"");
    }

    @Test
    void restaurantsRestaurantIdCourier() {
        sut.restaurantsPost(new RestaurantsPostRequest().restaurantID("bla").location(List.of(0.5, 0.1)));
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        ResponseEntity<Restaurant> r = sut.restaurantsRestaurantIdGet("bla", "thtrff");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().getRestaurantID()).isNull();
        assertThat(r.getBody().getDeliveryZone()).isNull();
    }

    @Test
    void restaurantsRestaurantIdCustomer() {
        sut.restaurantsPost(new RestaurantsPostRequest().restaurantID("bla").location(List.of(0.5, 0.1)));
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        ResponseEntity<Restaurant> r = sut.restaurantsRestaurantIdGet("bla", "thtrff");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().getRestaurantID()).isNull();
        assertThat(r.getBody().getDeliveryZone()).isNull();
    }

    @Test
    void restaurantsRestaurantIdVendorNotTheSame() {
        sut.restaurantsPost(new RestaurantsPostRequest().restaurantID("bla").location(List.of(0.5, 0.1)));
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.VENDOR);

        assertThatThrownBy(() -> sut.restaurantsRestaurantIdGet("bla", "duf"))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdGet("bla", "duf"))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
    }

    @Test
    void restaurantsRestaurantIdVENDORTheSame() {
        sut.restaurantsPost(new RestaurantsPostRequest().restaurantID("bla").location(List.of(0.5, 0.1)));
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        ResponseEntity<Restaurant> r = sut.restaurantsRestaurantIdGet("bla", "bla");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().getRestaurantID()).isEqualTo("bla");
    }

    @Test
    void restaurantsRestaurantIdAdmin() {
        sut.restaurantsPost(new RestaurantsPostRequest().restaurantID("bla").location(coord));
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        ResponseEntity<Restaurant> r = sut.restaurantsRestaurantIdGet("bla", "bla");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void restaurantsRestaurantIdInvalid() {
        sut.restaurantsPost(new RestaurantsPostRequest().restaurantID("bla").location(List.of(0.5, 0.1)));
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdGet("bla", "bla"))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdGet("bla", "bla"))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");
    }

    @Test
    void restaurantsRestaurantIdNotFound() {
        ResponseStatusException r = assertThrows(ResponseStatusException.class,
            () -> sut.restaurantsRestaurantIdGet("bla", "bla"));
        assertThat(r.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

    }

    @Test
    void restaurantsRestaurantCourierPutNull() {

        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut(null, "bla", null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut(null, "bla", null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID or Restaurant ID is invalid.\"");
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", null, null))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", null, null))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID or Restaurant ID is invalid.\"");
    }

    @Test
    void restaurantsRestaurantCourierClient() {
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
    }

    @Test
    void restaurantsRestaurantCourierCourier() {
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
    }

    @Test
    void restaurantsRestaurantCourierVendorNotTheSame() {
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "duf", null))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "duf", null))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
    }

    @Test
    void restaurantsRestaurantCourierVendorTheSame() {
        sut.restaurantsPost(new RestaurantsPostRequest().restaurantID("bla").location(List.of(0.5, 0.1)));
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        ResponseEntity<Restaurant> r = sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void restaurantsRestaurantCourierInvalid() {
        when(usersCommunication.getUserAccountType(any())).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");
    }

    @Test
    void restaurantsRestaurantCourierADMINOk() {
        sut.restaurantsPost(new RestaurantsPostRequest().restaurantID("bla").location(List.of(0.5, 0.1)));
        when(usersCommunication.getUserAccountType("bla")).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.getUserAccountType("bl")).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        List<String> list = new ArrayList<>();
        list.add("bl");
        ResponseEntity<Restaurant> r = sut.restaurantsRestaurantIdCouriersPut("bla", "bla", list);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void restaurantsRestaurantCourierAdminInvalid() {
        sut.restaurantsPost(new RestaurantsPostRequest().restaurantID("bla").location(List.of(0.5, 0.1)));
        when(usersCommunication.getUserAccountType("bla")).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.getUserAccountType("bl")).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        List<String> list = new ArrayList<>();
        list.add("bl");
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", list))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", list))
            .message()
            .isEqualTo("400 BAD_REQUEST \"List contains the id of someone who isn't a courier.\"");
    }

    @Test
    void restaurantsRestaurantCourierNoRestaurant() {
        when(usersCommunication.getUserAccountType("bla")).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
            .extracting("status")
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
            .message()
            .isEqualTo("500 INTERNAL_SERVER_ERROR \"Server error\"");
    }

}