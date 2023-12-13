package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.Delivery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

  @Mock
  private DeliveryService deliveryService;

  @Mock
  UsersCommunication usersCommunication;
  @InjectMocks
  private DeliveryController deliveryController;


  @Test
  void deliveriesDeliveryIdRatingCourierPutCustomer() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String userId = "hi_im_a_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String restaurantId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(restaurantId);
    m.setCustomerID(userId);
    String type = "customer";
    Integer rating = 5;

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(userId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
    doAnswer(invocation -> {
        Integer r = invocation.getArgument(1);
        m.setRatingCourier(r);
        return m;
    }).when(deliveryService).updateCourierRating(deliveryId, rating);

    ResponseEntity<Delivery> result = deliveryController.deliveriesDeliveryIdRatingCourierPut(deliveryId, userId, 5);

    Delivery resultBody = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(5, resultBody.getRatingCourier());

    verify(deliveryService, times(1)).updateCourierRating(deliveryId, rating);
    verify(deliveryService, times(2)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(userId);
  }

  @Test
  void deliveriesDeliveryIdRatingCourierPutAdmin() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String userId = "hi_im_an_admin@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String restaurantId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(restaurantId);
    m.setCustomerID(customerId);
    String type = "admin";
    Integer rating = 0;

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(userId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
    doAnswer(invocation -> {
      Integer r = invocation.getArgument(1);
      m.setRatingCourier(r);
      return m;
    }).when(deliveryService).updateCourierRating(deliveryId, rating);

    ResponseEntity<Delivery> result = deliveryController.deliveriesDeliveryIdRatingCourierPut(deliveryId, userId, 0);

    Delivery resultBody = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(0, resultBody.getRatingCourier());

    verify(deliveryService, times(1)).updateCourierRating(deliveryId, rating);
    verify(deliveryService, times(2)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(userId);
  }

  @Test
  void deliveriesDeliveryIdRatingCourierPutCourier() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String restaurantId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(restaurantId);
    m.setCustomerID(customerId);
    String type = "courier";
    Integer rating = 5;

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(courierId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);

    ResponseEntity<Delivery> result = deliveryController.deliveriesDeliveryIdRatingCourierPut(deliveryId, courierId, rating);

    assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

    verify(deliveryService, times(1)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(courierId);
  }

  @Test
  void deliveriesDeliveryIdRatingRestaurantPutCustomer() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String userId = "hi_im_a_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String restaurantId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(restaurantId);
    m.setCustomerID(userId);
    String type = "customer";
    Integer rating = 5;

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(userId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
    doAnswer(invocation -> {
      Integer r = invocation.getArgument(1);
      m.setRatingRestaurant(r);
      return m;
    }).when(deliveryService).updateRestaurantRating(deliveryId, rating);

    ResponseEntity<Delivery> result = deliveryController.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, 5);

    Delivery resultBody = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(5, resultBody.getRatingRestaurant());

    verify(deliveryService, times(1)).updateRestaurantRating(deliveryId, rating);
    verify(deliveryService, times(2)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(userId);
  }

  @Test
  void deliveriesDeliveryIdRatingRestaurantPutDiffCustomer() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String userId = "hi_im_a_different_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String restaurantId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(restaurantId);
    m.setCustomerID(customerId);
    String type = "customer";
    Integer rating = 5;

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(userId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);

    ResponseEntity<Delivery> result = deliveryController.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, rating);

    assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

    verify(deliveryService, times(1)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(userId);
  }

  @Test
  void deliveriesDeliveryIdRatingRestaurantPutAdmin() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String userId = "hi_im_an_admin@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String restaurantId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(restaurantId);
    m.setCustomerID(customerId);
    String type = "admin";
    Integer rating = 0;

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(userId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
    doAnswer(invocation -> {
      Integer r = invocation.getArgument(1);
      m.setRatingRestaurant(r);
      return m;
    }).when(deliveryService).updateRestaurantRating(deliveryId, rating);

    ResponseEntity<Delivery> result = deliveryController.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, 0);

    Delivery resultBody = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(0, resultBody.getRatingRestaurant());

    verify(deliveryService, times(1)).updateRestaurantRating(deliveryId, rating);
    verify(deliveryService, times(2)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(userId);
  }

  @Test
  void deliveriesDeliveryIdRatingRestaurantPutVendor() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String userId = "hi_im_a_different_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String restaurantId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(restaurantId);
    m.setCustomerID(customerId);
    String type = "customer";
    Integer rating = 5;

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(userId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);

    ResponseEntity<Delivery> result = deliveryController.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, rating);

    assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

    verify(deliveryService, times(1)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(userId);
  }

  @Test
  void deliveriesDeliveryIdRatingRestaurantGetVendor() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String restaurantId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(restaurantId);
    m.setCustomerID(customerId);
    String type = "vendor";

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(restaurantId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);

    ResponseEntity<Integer> result = deliveryController.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, restaurantId);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertNull(result.getBody());

    verify(deliveryService, times(1)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(restaurantId);
  }

  @Test
  void deliveriesDeliveryIdRatingRestaurantGetDiffVendor() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String vendorId = "hi_im_a_vendor@testmail.com";
    String restaurantId = "hi_im_a_rival_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(vendorId);
    m.setCustomerID(customerId);
    String type = "vendor";

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(restaurantId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);

    ResponseEntity<Integer> result = deliveryController.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, restaurantId);

    assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

    verify(deliveryService, times(1)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(restaurantId);
  }

  @Test
  void deliveriesDeliveryIdRatingRestaurantGetCustomer() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String vendorId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(vendorId);
    m.setCustomerID(customerId);
    String type = "customer";

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(customerId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);

    ResponseEntity<Integer> result = deliveryController.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, customerId);

    assertEquals(HttpStatus.OK, result.getStatusCode());

    verify(deliveryService, times(1)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(customerId);
  }

  @Test
  void deliveriesDeliveryIdRatingCourierGetCourier() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String vendorId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(vendorId);
    m.setCustomerID(customerId);
    String type = "courier";

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(courierId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);

    ResponseEntity<Integer> result = deliveryController.deliveriesDeliveryIdRatingCourierGet(deliveryId, courierId);

    assertEquals(HttpStatus.OK, result.getStatusCode());

    verify(deliveryService, times(1)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(courierId);
  }

  @Test
  void deliveriesDeliveryIdRatingCourierGetDiffCourier() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String diffCourierId = "hi_im_a_different_courier@testmail.com";
    String vendorId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(vendorId);
    m.setCustomerID(customerId);
    String type = "courier";

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(diffCourierId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);

    ResponseEntity<Integer> result = deliveryController.deliveriesDeliveryIdRatingCourierGet(deliveryId, diffCourierId);

    assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

    verify(deliveryService, times(1)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(diffCourierId);
  }

  @Test
  void deliveriesDeliveryIdRatingCourierGetCustomer() {
    //Mock data
    UUID deliveryId = UUID.randomUUID();
    String customerId = "hi_im_a_customer@testmail.com";
    String courierId = "hi_im_a_courier@testmail.com";
    String vendorId = "hi_im_a_vendor@testmail.com";
    Delivery m = new Delivery();
    m.setDeliveryID(deliveryId);
    m.setCourierID(courierId);
    m.setRestaurantID(vendorId);
    m.setCustomerID(customerId);
    String type = "customer";

    //Mock deliveryService/userRepo methods
    when(usersCommunication.getAccountType(customerId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);

    ResponseEntity<Integer> result = deliveryController.deliveriesDeliveryIdRatingCourierGet(deliveryId, customerId);

    assertEquals(HttpStatus.OK, result.getStatusCode());

    verify(deliveryService, times(1)).getDelivery(deliveryId);
    verify(usersCommunication, times(1)).getAccountType(customerId);
  }
}