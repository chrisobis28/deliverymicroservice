package nl.tudelft.sem.template.delivery.controllers.Delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import nl.tudelft.sem.template.delivery.controllers.DeliveryController;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.DeliveriesPostRequest;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@DataJpaTest
public class DeliveryControllerTestAutoWired {

  @Autowired
  private DeliveryRepository deliveryRepository;
  @Autowired
  private RestaurantRepository restaurantRepository;

  @Mock
  private UsersAuthenticationService usersAuthentication;

  private DeliveryController deliveryController;

  @BeforeEach
  public void init(){
    DeliveryService deliveryService = new DeliveryService(deliveryRepository, restaurantRepository);
    deliveryController = new DeliveryController(deliveryService, usersAuthentication, null);
  }

  @Test
  void deliveriesPostNull() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> deliveryController.deliveriesPost(null));
    assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
    assertEquals(exception.getReason(), "BAD REQUEST");
  }

  @Test
  void deliveriesPostInvalidStatus() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    dpr.setStatus("invalid");
    dpr.setCustomerId("user@testmail.com");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setOrderId(UUID.randomUUID().toString());
    dpr.setDeliveryAddress(List.of(50.4, 32.6));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> deliveryController.deliveriesPost(dpr));
    assertEquals(exception.getMessage(), "Unexpected value 'INVALID'");
  }

  @Test
  void deliveriesPostEmptyEmail() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    dpr.setStatus("pending");
    dpr.setCustomerId("user@testmail.com");
    dpr.setOrderId(UUID.randomUUID().toString());
    dpr.setDeliveryAddress(List.of(50.4, 32.6));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> deliveryController.deliveriesPost(dpr));
    assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
    assertEquals(exception.getReason(), "BAD REQUEST");
  }

  @Test
  void deliveriesPostEmptyAddress() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    dpr.setStatus("pending");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setCustomerId("user@testmail.com");
    dpr.setOrderId(UUID.randomUUID().toString());
    dpr.setDeliveryAddress(new ArrayList<>());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> deliveryController.deliveriesPost(dpr));
    assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
    assertEquals(exception.getReason(), "BAD REQUEST");
  }

  @Test
  void deliveriesPostNullAddress() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    dpr.setStatus("pending");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setCustomerId("user@testmail.com");
    dpr.setOrderId(UUID.randomUUID().toString());
    dpr.setDeliveryAddress(null);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> deliveryController.deliveriesPost(dpr));
    assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
    assertEquals(exception.getReason(), "BAD REQUEST");
  }

  @Test
  void deliveriesPostVendorNotFound() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    dpr.setStatus("pending");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setCustomerId("user@testmail.com");
    dpr.setOrderId(UUID.randomUUID().toString());
    dpr.setDeliveryAddress(List.of(40.0, 30.0));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> deliveryController.deliveriesPost(dpr));
    assertEquals(exception.getStatus(), HttpStatus.NOT_FOUND);
    assertEquals(exception.getReason(), "VENDOR NOT FOUND.");
  }

  @Test
  void deliveriesPostOutOfDeliveryZone() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    String orderId = UUID.randomUUID().toString();
    dpr.setStatus("accepted");
    dpr.setCustomerId("user@testmail.com");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setOrderId(orderId);
    dpr.setDeliveryAddress(List.of(50.4, 32.6));

    Restaurant r = new Restaurant();
    r.setLocation(List.of(40.1, 32.4));
    r.setRestaurantID("hi_im_a_vendor@testmail.com");

    restaurantRepository.save(r);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> deliveryController.deliveriesPost(dpr));
    assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST);
    assertEquals(exception.getReason(), "CUSTOMER OUTSIDE THE VENDOR DELIVERY ZONE.");
  }

  @Test
  void deliveriesPostPending() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    String orderId = UUID.randomUUID().toString();
    dpr.setStatus("pending");
    dpr.setCustomerId("user@testmail.com");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setOrderId(orderId);
    dpr.setDeliveryAddress(List.of(50.4, 32.6));

    Restaurant r = new Restaurant();
    r.setLocation(List.of(50.3, 32.4));
    r.setRestaurantID("hi_im_a_vendor@testmail.com");

    restaurantRepository.save(r);
    ResponseEntity<Delivery> result = deliveryController.deliveriesPost(dpr);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
  }

  @Test
  void deliveriesPostAccepted() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    String orderId = UUID.randomUUID().toString();
    dpr.setStatus("accepted");
    dpr.setCustomerId("user@testmail.com");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setOrderId(orderId);
    dpr.setDeliveryAddress(List.of(50.4, 32.6));

    Restaurant r = new Restaurant();
    r.setLocation(List.of(50.3, 32.4));
    r.setRestaurantID("hi_im_a_vendor@testmail.com");

    restaurantRepository.save(r);
    ResponseEntity<Delivery> result = deliveryController.deliveriesPost(dpr);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
  }

  @Test
  void deliveriesPostRejected() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    String orderId = UUID.randomUUID().toString();
    dpr.setStatus("Rejected");
    dpr.setCustomerId("user@testmail.com");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setOrderId(orderId);
    dpr.setDeliveryAddress(List.of(50.4, 32.6));

    Restaurant r = new Restaurant();
    r.setLocation(List.of(50.3, 32.4));
    r.setRestaurantID("hi_im_a_vendor@testmail.com");

    restaurantRepository.save(r);
    ResponseEntity<Delivery> result = deliveryController.deliveriesPost(dpr);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
  }

  @Test
  void deliveriesPostPreparing() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    String orderId = UUID.randomUUID().toString();
    dpr.setStatus("PREPARING");
    dpr.setCustomerId("user@testmail.com");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setOrderId(orderId);
    dpr.setDeliveryAddress(List.of(50.4, 32.6));

    Restaurant r = new Restaurant();
    r.setLocation(List.of(50.3, 32.4));
    r.setRestaurantID("hi_im_a_vendor@testmail.com");

    restaurantRepository.save(r);
    ResponseEntity<Delivery> result = deliveryController.deliveriesPost(dpr);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
  }

  @Test
  void deliveriesPostGivenToCourier() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    String orderId = UUID.randomUUID().toString();
    dpr.setStatus("Given_to_Courier");
    dpr.setCustomerId("user@testmail.com");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setOrderId(orderId);
    dpr.setDeliveryAddress(List.of(50.4, 32.6));

    Restaurant r = new Restaurant();
    r.setLocation(List.of(50.3, 32.4));
    r.setRestaurantID("hi_im_a_vendor@testmail.com");

    restaurantRepository.save(r);
    ResponseEntity<Delivery> result = deliveryController.deliveriesPost(dpr);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
  }

  @Test
  void deliveriesPostOnTransit() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    String orderId = UUID.randomUUID().toString();
    dpr.setStatus("ON_TRANSIT");
    dpr.setCustomerId("user@testmail.com");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setOrderId(orderId);
    dpr.setDeliveryAddress(List.of(50.4, 32.6));

    Restaurant r = new Restaurant();
    r.setLocation(List.of(50.3, 32.4));
    r.setRestaurantID("hi_im_a_vendor@testmail.com");

    restaurantRepository.save(r);
    ResponseEntity<Delivery> result = deliveryController.deliveriesPost(dpr);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
  }

  @Test
  void deliveriesPostDELIVERED() {
    DeliveriesPostRequest dpr = new DeliveriesPostRequest();
    String orderId = UUID.randomUUID().toString();
    dpr.setStatus("delivered");
    dpr.setCustomerId("user@testmail.com");
    dpr.setVendorId("hi_im_a_vendor@testmail.com");
    dpr.setOrderId(orderId);
    dpr.setDeliveryAddress(List.of(50.4, 32.6));

    Restaurant r = new Restaurant();
    r.setLocation(List.of(50.3, 32.4));
    r.setRestaurantID("hi_im_a_vendor@testmail.com");

    restaurantRepository.save(r);
    ResponseEntity<Delivery> result = deliveryController.deliveriesPost(dpr);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
  }
}
