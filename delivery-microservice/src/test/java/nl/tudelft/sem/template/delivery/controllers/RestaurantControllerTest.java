package nl.tudelft.sem.template.delivery.controllers;

import java.util.UUID;
import nl.tudelft.sem.template.delivery.AddressAdapter;
import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import nl.tudelft.sem.template.model.RestaurantsPostRequest;
import org.h2.engine.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
//import static org.mockito.Mockito.when;

class RestaurantControllerTest {

  private TestDeliveryRepository repo1;
  private TestRestaurantRepository repo2;
  private RestaurantController sut;
  private UsersAuthenticationService usersCommunication;

  List<String> addr;
  List<Double> co_ord;

  @BeforeEach
  void setUp() {
    // Mock data
    addr = List.of("NL","1234AB","Amsterdam","Kalverstraat","36B");
    co_ord = List.of(32.6, 50.4);

    usersCommunication = mock(UsersAuthenticationService.class);

    repo1 = new TestDeliveryRepository();
    repo2 = new TestRestaurantRepository();
    sut = new RestaurantController(new RestaurantService(repo2, repo1), new AddressAdapter(new GPS()), usersCommunication);
  }

  @Test
  void restaurantsPostNullRpr() {
    ResponseEntity<Restaurant> result = sut.restaurantsPost(null);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
  }

  @Test
  void restaurantsInvalidAddr() {
    RestaurantsPostRequest rpr = new RestaurantsPostRequest();
    rpr.setRestaurantID("hi_im_a_vendor@testmail.com");
    rpr.setLocation(List.of());

    ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
  }

  @Test
  void restaurantsInvalidEmail() {
    RestaurantsPostRequest rpr = new RestaurantsPostRequest();
    rpr.setRestaurantID(null);
    rpr.setLocation(addr);

    ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
  }

  @Test
  void restaurantsEmptyEmail() {
    RestaurantsPostRequest rpr = new RestaurantsPostRequest();
    rpr.setRestaurantID("");
    rpr.setLocation(addr);

    ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
  }

  @Test
  void restaurantsInvalidAddr2() {
    RestaurantsPostRequest rpr = new RestaurantsPostRequest();
    rpr.setRestaurantID("hi_im_a_vendor@testmail.com");
    rpr.setLocation(List.of("NL","1234AB","Amsterdam",""," "));

    ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
  }

  @Test
  void restaurantsPost() {
    RestaurantsPostRequest rpr = new RestaurantsPostRequest();
    rpr.setRestaurantID("hi_im_a_vendor@testmail.com");
    rpr.setLocation(addr);
    //when(sut.mockGPS.getCoordinatesOfAddress(addr)).thenReturn(co_ord);
    Restaurant r = new Restaurant();
    r.setRestaurantID("hi_im_a_vendor@testmail.com");
    r.setLocation(co_ord);
    //sut.insert(r);

    ResponseEntity<Restaurant> result = sut.restaurantsPost(rpr);
    Restaurant added = result.getBody();

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertTrue(added.getLocation().get(0) > 52.3);
    assertTrue(added.getLocation().get(1) > 4.97);
  }

  @Test
  void restaurantsRestaurantIdLocationPutAdmin(){
    String restaurantId = "restaurant_admin@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setLocation(List.of(0.0, 0.0));
    sut.insert(r);

    String userId = "user_admin@testmail.com";
    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
    when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

    ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1));
    assertEquals(HttpStatus.OK, res.getStatusCode());
    assertNotNull(res.getBody());
    assertEquals(res.getBody().getLocation(), List.of(0.1, 0.1));
  }

  @Test
  void restaurantsRestaurantIdLocationPutSameVendor(){
    String restaurantId = "restaurant_sameVendor@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setLocation(List.of(0.0, 0.0));
    sut.insert(r);

    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
    when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);

    ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdLocationPut(restaurantId, restaurantId, List.of(0.1, 0.1));
    assertEquals(HttpStatus.OK, res.getStatusCode());
    assertNotNull(res.getBody());
    assertEquals(res.getBody().getLocation(), List.of(0.1, 0.1));
  }

  @Test
  void restaurantsRestaurantIdLocationPutDiffVendor(){
    String restaurantId = "restaurant_diffVendor@testmail.com";
    String otherRestaurantId = "other_restaurant_diffVendor@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setLocation(List.of(0.0, 0.0));
    sut.insert(r);

    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
    when(usersCommunication.getUserAccountType(otherRestaurantId)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, otherRestaurantId, List.of(0.1, 0.1)));
    assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
    assertEquals(exception.getReason(), "FORBIDDEN");
  }

  @Test
  void restaurantsRestaurantIdLocationPutCourier(){
    String restaurantId = "restaurant_courier@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setLocation(List.of(0.0, 0.0));
    sut.insert(r);

    String userId = "user_courier@testmail.com";
    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
    when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
    assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
    assertEquals(exception.getReason(), "FORBIDDEN");
  }

  @Test
  void restaurantsRestaurantIdLocationPutClient(){
    String restaurantId = "restaurant_client@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setLocation(List.of(0.0, 0.0));
    sut.insert(r);

    String userId = "user_client@testmail.com";
    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
    when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
    assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
    assertEquals(exception.getReason(), "FORBIDDEN");
  }

  @Test
  void restaurantsRestaurantIdLocationPutInvalid(){
    String restaurantId = "restaurant_invalid@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setLocation(List.of(0.0, 0.0));
    sut.insert(r);

    String userId = "user_invalid@testmail.com";
    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;
    when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
    assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
    assertEquals(exception.getReason(), "UNAUTHORIZED");
  }

  @Test
  void restaurantsRestaurantIdLocationPutNotFound(){
    String restaurantId = "restaurant_not_found@testmail.com";

    String userId = "user_not_found@testmail.com";
    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
    when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdLocationPut(restaurantId, userId, List.of(0.1, 0.1)));
    assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
    assertEquals(exception.getReason(), "Restaurant with specified id not found");
  }

  @Test
  void restaurantsRestaurantIdDeliverZonePutAdmin(){
    String restaurantId = "restaurant_admin@testmail.com";
    Restaurant r = new Restaurant();
    r.setLocation(List.of(6.7,6.7));
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
  void restaurantsRestaurantIdDeliverZonePutSameVendor(){
    String restaurantId = "restaurant_sameVendor@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setLocation(List.of(1.2,3.4));
    r.setDeliveryZone(10.0);
    sut.insert(r);

    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
    when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);

    ResponseEntity<Restaurant> res = sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, restaurantId, 20.0);
    assertEquals(HttpStatus.OK, res.getStatusCode());
    assertNotNull(res.getBody());
    assertEquals(res.getBody().getDeliveryZone(), 20.0);
  }

  @Test
  void restaurantsRestaurantIdDeliverZonePutDiffVendor(){
    String restaurantId = "restaurant_diffVendor@testmail.com";
    String otherRestaurantId = "other_restaurant_diffVendor@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setDeliveryZone(10.0);
    sut.insert(r);

    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
    when(usersCommunication.getUserAccountType(otherRestaurantId)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, otherRestaurantId, 20.0));
    assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
    assertEquals(exception.getReason(), "FORBIDDEN");
  }

  @Test
  void restaurantsRestaurantIdDeliverZonePutCourier(){
    String restaurantId = "restaurant_courier@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setDeliveryZone(10.0);
    sut.insert(r);

    String userId = "user_courier@testmail.com";
    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
    when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
    assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
    assertEquals(exception.getReason(), "FORBIDDEN");
  }

  @Test
  void restaurantsRestaurantIdDeliverZonePutClient(){
    String restaurantId = "restaurant_client@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setDeliveryZone(10.0);
    sut.insert(r);

    String userId = "user_client@testmail.com";
    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
    when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
    assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
    assertEquals(exception.getReason(), "FORBIDDEN");
  }

  @Test
  void restaurantsRestaurantIdDeliverZonePutInvalid(){
    String restaurantId = "restaurant_invalid@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setDeliveryZone(10.0);
    sut.insert(r);

    String userId = "user_invalid@testmail.com";
    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;
    when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
    assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
    assertEquals(exception.getReason(), "UNAUTHORIZED");
  }

  @Test
  void restaurantsRestaurantIdDeliverZonePutNotFound(){
    String restaurantId = "restaurant_not_found@testmail.com";

    String userId = "user_not_found@testmail.com";
    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
    when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdDeliverZonePut(restaurantId, userId, 20.0));
    assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
    assertEquals(exception.getReason(), "Restaurant with specified id not found");
  }

  @Test
  void restaurantsRestaurantIdNewOrdersGetAdmin(){
    String restaurantId = "restaurant_neworders_admin@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
    r.setLocation(List.of(6.7,6.7));
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
  void restaurantsRestaurantIdNewOrdersGetSameVendor(){
    String restaurantId = "restaurant_neworders_same_vendor@testmail.com";
    Restaurant r = new Restaurant();
    r.setLocation(List.of(5.6,67.7));
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

    String userId = "user_admin@testmail.com";
    UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
    when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);

    ResponseEntity<List<Delivery>> res = sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, restaurantId);
    assertEquals(HttpStatus.OK, res.getStatusCode());
    assertNotNull(res.getBody());
    assertEquals(res.getBody().get(0).getDeliveryID(), deliveryId2);
    assertEquals(res.getBody().size(), 1);
  }

  @Test
  void restaurantsRestaurantIdNewOrdersGetDiffVendor(){
    String restaurantId = "restaurant_neworders_diff_vendor@testmail.com";
    String other_restaurant = "restaurant_neworders_diff_vendor_other@testmail.com";
    Restaurant r = new Restaurant();
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
    when(usersCommunication.getUserAccountType(other_restaurant)).thenReturn(type);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, other_restaurant));
    assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
    assertEquals(exception.getReason(), "FORBIDDEN");
  }

  @Test
  void restaurantsRestaurantIdNewOrdersGetClient(){
    String restaurantId = "restaurant_neworders_client@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
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

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, userId));
    assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
    assertEquals(exception.getReason(), "FORBIDDEN");
  }

  @Test
  void restaurantsRestaurantIdNewOrdersGetCourier(){
    String restaurantId = "restaurant_neworders_courier@testmail.com";
    Restaurant r = new Restaurant();
    r.setRestaurantID(restaurantId);
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

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, userId));
    assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
    assertEquals(exception.getReason(), "FORBIDDEN");
  }

  @Test
  void restaurantsRestaurantIdNewOrdersGetNotFound(){
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

    ResponseStatusException exception = assertThrows(RestaurantService.RestaurantNotFoundException.class, () -> sut.restaurantsRestaurantIdNewOrdersGet(restaurantId, userId));
    assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
    assertEquals(exception.getReason(), "Restaurant with specified id not found");
  }
}