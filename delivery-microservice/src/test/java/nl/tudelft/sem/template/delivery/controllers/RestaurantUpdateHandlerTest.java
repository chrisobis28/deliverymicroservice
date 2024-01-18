//package nl.tudelft.sem.template.delivery.controllers;
//
//import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
//import nl.tudelft.sem.template.delivery.services.UpdateRestaurantService;
//import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
//import nl.tudelft.sem.template.model.Restaurant;
//import org.apache.commons.lang3.tuple.Pair;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.domain.EntityScan;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.server.ResponseStatusException;
//
//import javax.transaction.Transactional;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@EntityScan("nl.tudelft.sem.template.*")
//@ExtendWith(MockitoExtension.class)
//@Transactional
//@DataJpaTest
//class RestaurantUpdateHandlerTest {
//
//    @Autowired
//    private RestaurantRepository repo2;
//    private RestaurantUpdateHandler sut;
//    private UpdateRestaurantService rs;
//    private UsersAuthenticationService usersCommunication;
//
//    List<Double> coord;
//
//    @BeforeEach
//    void setUp() {
//        coord = List.of(32.6, 50.4);
//        rs = new UpdateRestaurantService(repo2);
//        usersCommunication = mock(UsersAuthenticationService.class);
//        sut = new RestaurantUpdateHandler(rs, usersCommunication);
//    }
//
//    @Test
//    void restaurantsRestaurantIdDeliverZonePutDiffVendor() {
//        String restaurantId = "restaurant_diffVendor@testmail.com";
//        String otherRestaurantId = "other_restaurant_diffVendor@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        r.setLocation(coord);
//        r.setDeliveryZone(10.0);
//        repo2.save(r);
//
//        String msg = "User lacks necessary permissions.";
//        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
//        //when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);
//        when(usersCommunication.getUserAccountType(otherRestaurantId)).thenReturn(type);
//        when(usersCommunication.checkUserAccessToRestaurant(otherRestaurantId, restaurantId, "Delivery Zone"))
//            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, otherRestaurantId, 20.0));
//        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
//        assertEquals(exception.getReason(), "User lacks necessary permissions.");
//    }
//
//    @Test
//    void restaurantsRestaurantIdDeliverZonePutClient() {
//        String restaurantId = "restaurant_client@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        r.setLocation(List.of(0.1, 0.2));
//        r.setDeliveryZone(10.0);
//        repo2.save(r);
//
//        String userId = "user_client@testmail.com";
//        String msg = "Only vendors and admins can change the delivery zone of a restaurant.";
//        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
//        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
//        when(usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Delivery Zone"))
//            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
//        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
//        assertEquals(exception.getReason(), "Only vendors and admins can change the delivery zone of a restaurant.");
//    }
//
//    @Test
//    void restaurantsRestaurantIdDeliverZonePutCourier() {
//        String restaurantId = "restaurant_courier@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        r.setDeliveryZone(10.0);
//        repo2.save(r);
//
//        String userId = "user_courier@testmail.com";
//        String msg = "Only vendors and admins can change the delivery zone of a restaurant.";
//        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
//        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
//        when(usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Delivery Zone"))
//            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
//        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
//        assertEquals(exception.getReason(), "Only vendors and admins can change the delivery zone of a restaurant.");
//    }
//
//    @Test
//    void restaurantsRestaurantIdDeliverZonePutInvalid() {
//        String restaurantId = "restaurant_invalid@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        r.setLocation(coord);
//        r.setDeliveryZone(10.0);
//        repo2.save(r);
//
//        String userId = "user_invalid@testmail.com";
//        String msg = "User lacks valid authentication credentials.";
//        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;
//        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
//        when(usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Delivery Zone"))
//            .thenReturn(Pair.of(HttpStatus.UNAUTHORIZED, msg));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
//        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
//        assertEquals(exception.getReason(), "User lacks valid authentication credentials.");
//    }
//
//    @Test
//    void restaurantsRestaurantIdDeliverZonePutNotFound() {
//        String restaurantId = "restaurant_not_found@testmail.com";
//
//        String userId = "user_not_found@testmail.com";
//        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
//        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
//        when(usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Delivery Zone"))
//            .thenReturn(Pair.of(HttpStatus.OK, "OK"));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
//        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
//        assertEquals(exception.getReason(), "Restaurant with specified id not found");
//    }
//
//    @Test
//    void restaurantsRestaurantIdDeliverZonePutAdmin() {
//        String restaurantId = "restaurant_admin@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setLocation(List.of(6.7, 6.7));
//        r.setRestaurantID(restaurantId);
//        r.setDeliveryZone(10.0);
//        repo2.save(r);
//
//        String userId = "user_admin@testmail.com";
//        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
//        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
//        when(usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Delivery Zone"))
//            .thenReturn(Pair.of(HttpStatus.OK, "OK"));
//
//        ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0);
//        assertEquals(HttpStatus.OK, res.getStatusCode());
//        assertNotNull(res.getBody());
//        assertEquals(res.getBody().getDeliveryZone(), 20.0);
//    }
//
//    @Test
//    void restaurantsRestaurantIdDeliverZonePutEmptyCourierList() {
//        String restaurantId = "restaurant_sameVendor@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        r.setDeliveryZone(10.0);
//        r.setLocation(coord);
//        r.setCouriers(List.of());
//        repo2.save(r);
//
//        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
//        when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);
//        //when(usersCommunication.getUserAccountType(otherRestaurantId)).thenReturn(type);
//        when(usersCommunication.checkUserAccessToRestaurant(restaurantId, restaurantId, "Delivery Zone"))
//            .thenReturn(Pair.of(HttpStatus.OK, "OK"));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, restaurantId, 20.0));
//        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
//        assertEquals(exception.getReason(), "User lacks necessary permissions.");
//    }
//
//    @Test
//    void restaurantsRestaurantIdDeliverZonePutSameVendor() {
//        String restaurantId = "restaurant_sameVendor@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        r.setLocation(List.of(1.2, 3.4));
//        r.setDeliveryZone(10.0);
//        r.setCouriers(List.of("example_courier@testmail.com"));
//        repo2.save(r);
//
//        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
//        when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);
//        when(usersCommunication.checkUserAccessToRestaurant(restaurantId, restaurantId, "Delivery Zone"))
//            .thenReturn(Pair.of(HttpStatus.OK, "OK"));
//
//        ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, restaurantId, 20.0);
//        assertEquals(HttpStatus.OK, res.getStatusCode());
//        assertNotNull(res.getBody());
//        assertEquals(res.getBody().getDeliveryZone(), 20.0);
//    }
//
//    @Test
//    void restaurantsRestaurantIdLocationPutAdmin() {
//        String restaurantId = "restaurant_admin@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        List<Double> list = new ArrayList<>();
//        list.add(0.0);
//        list.add(0.0);
//        List<Double> list2 = new ArrayList<>();
//        list2.add(0.1);
//        list2.add(0.1);
//        r.setLocation(list);
//        repo2.save(r);
//
//        String userId = "user_admin@testmail.com";
//        when(usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Location"))
//            .thenReturn(Pair.of(HttpStatus.OK, "OK"));
//
//        ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, list2);
//        assertEquals(HttpStatus.OK, res.getStatusCode());
//        assertNotNull(res.getBody());
//        assertEquals(res.getBody().getLocation(), List.of(0.1, 0.1));
//    }
//
//    @Test
//    void restaurantsRestaurantIdLocationPutSameVendor() {
//        String restaurantId = "restaurant_sameVendor@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        List<Double> list = new ArrayList<>();
//        list.add(0.0);
//        list.add(0.0);
//        List<Double> list2 = new ArrayList<>();
//        list2.add(0.1);
//        list2.add(0.1);
//        r.setLocation(list);
//        repo2.save(r);
//
//        when(usersCommunication.checkUserAccessToRestaurant(restaurantId, restaurantId, "Location"))
//            .thenReturn(Pair.of(HttpStatus.OK, "OK"));
//
//        ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdLocationPut(restaurantId, restaurantId, list2);
//        assertEquals(HttpStatus.OK, res.getStatusCode());
//        assertNotNull(res.getBody());
//        assertEquals(res.getBody().getLocation(), List.of(0.1, 0.1));
//    }
//
//    @Test
//    void restaurantsRestaurantIdLocationPutDiffVendor() {
//        String restaurantId = "restaurant_diffVendor@testmail.com";
//        String otherRestaurantId = "other_restaurant_diffVendor@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        r.setLocation(List.of(0.0, 0.0));
//        repo2.save(r);
//
//        String msg = "User lacks necessary permissions.";
//        when(usersCommunication.checkUserAccessToRestaurant(otherRestaurantId, restaurantId, "Location"))
//            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, otherRestaurantId, List.of(0.1, 0.1)));
//        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
//        assertEquals(exception.getReason(), "User lacks necessary permissions.");
//    }
//
//    @Test
//    void restaurantsRestaurantIdLocationPutCourier() {
//        String restaurantId = "restaurant_courier@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        r.setLocation(List.of(0.0, 0.0));
//        repo2.save(r);
//
//        String userId = "user_courier@testmail.com";
//        String msg = "Only vendors and admins can change the restaurant's address";
//        when(usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Location"))
//            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
//        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
//        assertEquals(exception.getReason(), "Only vendors and admins can change the restaurant's address");
//    }
//
//    @Test
//    void restaurantsRestaurantIdLocationPutClient() {
//        String restaurantId = "restaurant_client@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        r.setLocation(List.of(0.0, 0.0));
//        repo2.save(r);
//
//        String userId = "user_client@testmail.com";
//        String msg = "Only vendors and admins can change the restaurant's address";
//        when(usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Location"))
//            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
//        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
//        assertEquals(exception.getReason(), "Only vendors and admins can change the restaurant's address");
//    }
//
//    @Test
//    void restaurantsRestaurantIdLocationPutInvalid() {
//        String restaurantId = "restaurant_invalid@testmail.com";
//        Restaurant r = new Restaurant();
//        r.setRestaurantID(restaurantId);
//        r.setLocation(List.of(0.0, 0.0));
//        repo2.save(r);
//
//        String userId = "user_invalid@testmail.com";
//        String msg = "User lacks valid authentication credentials.";
//        when(usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Location"))
//            .thenReturn(Pair.of(HttpStatus.UNAUTHORIZED, msg));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
//        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
//        assertEquals(exception.getReason(), "User lacks valid authentication credentials.");
//    }
//
//    @Test
//    void restaurantsRestaurantIdLocationPutNotFound() {
//        String restaurantId = "restaurant_not_found@testmail.com";
//
//        String userId = "user_not_found@testmail.com";
//        when(usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Location"))
//            .thenReturn(Pair.of(HttpStatus.OK, "OK"));
//
//        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
//            () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
//        assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
//        assertEquals(exception.getReason(), "Restaurant with specified id not found");
//    }
//
//    @Test
//    void restaurantsRestaurantCourierPutNull() {
//        String msg = "User ID or Restaurant ID is invalid.";
//        when(usersCommunication.checkUserAccessToRestaurant(any(), any(), any()))
//            .thenReturn(Pair.of(HttpStatus.BAD_REQUEST, msg));
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut(null, "bla", null))
//            .extracting("status")
//            .isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut(null, "bla", null))
//            .message()
//            .isEqualTo("400 BAD_REQUEST \"User ID or Restaurant ID is invalid.\"");
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", null, null))
//            .extracting("status")
//            .isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", null, null))
//            .message()
//            .isEqualTo("400 BAD_REQUEST \"User ID or Restaurant ID is invalid.\"");
//    }
//
//    @Test
//    void restaurantsRestaurantCourierClient() {
//        Restaurant r = new Restaurant();
//        r.setRestaurantID("bla");
//        r.setLocation(List.of(1.2, 3.4));
//        r.setDeliveryZone(10.0);
//        repo2.save(r);
//        String msg = "User lacks necessary permissions.";
//
//        when(usersCommunication.checkUserAccessToRestaurant(any(), any(), any()))
//            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla",
//            "bla", null))
//            .extracting("status")
//            .isEqualTo(HttpStatus.FORBIDDEN);
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla",
//            null))
//            .message()
//            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
//    }
//
//    @Test
//    void restaurantsRestaurantCourierCourier() {
//        Restaurant r = new Restaurant();
//        r.setRestaurantID("bla");
//        r.setLocation(List.of(1.2, 3.4));
//        r.setDeliveryZone(10.0);
//        repo2.save(r);
//
//        String msg = "User lacks necessary permissions.";
//        when(usersCommunication.checkUserAccessToRestaurant(any(), any(), any()))
//            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
//            .extracting("status")
//            .isEqualTo(HttpStatus.FORBIDDEN);
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
//            .message()
//            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
//    }
//
//    @Test
//    void restaurantsRestaurantCourierVendorNotTheSame() {
//        Restaurant r = new Restaurant();
//        r.setRestaurantID("bla");
//        r.setLocation(List.of(1.2, 3.4));
//        r.setDeliveryZone(10.0);
//        repo2.save(r);
//        String msg = "User lacks necessary permissions.";
//        when(usersCommunication.checkUserAccessToRestaurant(any(), any(), any()))
//            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "duf", null))
//            .extracting("status")
//            .isEqualTo(HttpStatus.FORBIDDEN);
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "duf", null))
//            .message()
//            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
//    }
//
//    @Test
//    void restaurantsRestaurantCourierVendorTheSame() {
//        repo2.save(new Restaurant().restaurantID("bla").location(List.of(0.5, 0.1)));
//        when(usersCommunication.checkUserAccessToRestaurant("bla", "bla", "Couriers"))
//            .thenReturn(Pair.of(HttpStatus.OK, "OK"));
//        ResponseEntity<Restaurant> r2 = sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null);
//        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.OK);
//    }
//
//    @Test
//    void restaurantsRestaurantCourierInvalid() {
//        Restaurant r = new Restaurant();
//        r.setRestaurantID("bla");
//        r.setLocation(List.of(1.2, 3.4));
//        r.setDeliveryZone(10.0);
//        repo2.save(r);
//
//        String msg = "User lacks valid authentication credentials.";
//        when(usersCommunication.checkUserAccessToRestaurant(any(), any(), any()))
//            .thenReturn(Pair.of(HttpStatus.UNAUTHORIZED, msg));
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
//            .extracting("status")
//            .isEqualTo(HttpStatus.UNAUTHORIZED);
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
//            .message()
//            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");
//    }
//
//    @Test
//    void restaurantsRestaurantCourierAdminOk() {
//        repo2.save(new Restaurant().restaurantID("bla").location(List.of(0.5, 0.1)));
//        when(usersCommunication.getUserAccountType("bl")).thenReturn(UsersAuthenticationService.AccountType.COURIER);
//        when(usersCommunication.checkUserAccessToRestaurant(any(), any(), any())).thenReturn(Pair.of(HttpStatus.OK, "OK"));
//        List<String> list = new ArrayList<>();
//        list.add("bl");
//        ResponseEntity<Restaurant> r = sut.restaurantsRestaurantIdCouriersPut("bla", "bla", list);
//        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
//    }
//
//    @Test
//    void restaurantsRestaurantCourierAdminInvalid() {
//        repo2.save(new Restaurant().restaurantID("bla").location(List.of(0.5, 0.1)));
//        when(usersCommunication.checkUserAccessToRestaurant(any(), any(), any())).thenReturn(Pair.of(HttpStatus.OK, "OK"));
//        when(usersCommunication.getUserAccountType("bl")).thenReturn(UsersAuthenticationService.AccountType.INVALID);
//        List<String> list = new ArrayList<>();
//        list.add("bl");
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", list))
//            .extracting("status")
//            .isEqualTo(HttpStatus.BAD_REQUEST);
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", list))
//            .message()
//            .isEqualTo("400 BAD_REQUEST \"List contains the id of someone who isn't a courier.\"");
//    }
//
//    @Test
//    void restaurantsRestaurantCourierNoRestaurant() {
//        when(usersCommunication.checkUserAccessToRestaurant(any(), any(), any())).thenReturn(Pair.of(HttpStatus.OK, "OK"));
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
//            .extracting("status")
//            .isEqualTo(HttpStatus.NOT_FOUND);
//        assertThatThrownBy(() -> sut.restaurantsRestaurantIdCouriersPut("bla", "bla", null))
//            .message()
//            .isEqualTo("404 NOT_FOUND \"Restaurant with specified id not found\"");
//    }
//}