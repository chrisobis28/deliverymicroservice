package nl.tudelft.sem.template.delivery.controllers;

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
  UserRepository userRepoMock;
  @InjectMocks
  private DeliveryController deliveryController;


  @Test
  void deliveriesDeliveryIdRatingCourierPut() {
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

    //Mock deliveryService/userRepo methods
    when(userRepoMock.getUserType(userId)).thenReturn(type);
    when(deliveryService.getDelivery(deliveryId)).thenReturn(m);

    ResponseEntity<Delivery> result = deliveryController.deliveriesDeliveryIdRatingCourierPut(deliveryId, userId, 5);

    Delivery resultBody = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(5, resultBody.getRatingCourier());
  }

  @Test
  void deliveriesDeliveryIdRatingRestaurantPut() {
  }

  @Test
  void deliveriesDeliveryIdRatingRestaurantGet() {
  }

  @Test
  void deliveriesDeliveryIdRatingCourierGet() {
  }
}