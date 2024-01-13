package nl.tudelft.sem.template.delivery.controllers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import nl.tudelft.sem.template.delivery.AddressAdapter;
import nl.tudelft.sem.template.delivery.GPS;
//import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.controllers.DeliveryController;
import nl.tudelft.sem.template.delivery.controllers.RestaurantController;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.DeliveriesPostRequest;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import nl.tudelft.sem.template.model.Restaurant;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
@EntityScan("nl.tudelft.sem.template.*")
@Transactional
@DataJpaTest
@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    public UsersAuthenticationService usersCommunication;

    private DeliveryController sut;
    @Autowired
    private DeliveryRepository repo1;
    @Autowired
    private RestaurantRepository repo2;
    private RestaurantController restaurantController;

    String userId;
    UsersAuthenticationService.AccountType userType;
    UUID deliveryId;
    Integer prepTime;
    Delivery delivery;
    List<Double> coords;

    @BeforeEach
    void setUp() {
        coords = List.of(100.0, 100.0);
        userId = "user@example.org";
        deliveryId = UUID.randomUUID();
        prepTime = 25;
        delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setEstimatedPrepTime(prepTime);
        usersCommunication = mock(UsersAuthenticationService.class);
        restaurantController = new RestaurantController(new RestaurantService(repo2, repo1), new AddressAdapter(new GPS()), usersCommunication);
        sut = new DeliveryController(new DeliveryService(repo1, repo2), usersCommunication, null);
    }

    @Test
    void deliveriesDeliveryIdPrepPut() {
        // Mock data
        userType = UsersAuthenticationService.AccountType.VENDOR;

        // Mock ratings and user type
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        sut.insert(delivery);
        // Call the method
        ResponseEntity<Delivery> responseEntity = sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime);

        // Verify the response
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify the returned delivery
        Delivery returned = responseEntity.getBody();
        assertEquals(returned, delivery);

        // Verify that we called the service methods and checked the user type
        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdPrepPutForbidden() {
        // Mock data
        userType = UsersAuthenticationService.AccountType.COURIER;

        // Mock ratings and user type
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        // Call the method
        ResponseEntity<Delivery> responseEntity = sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime);

        // Verify the response
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());

        // Verify the returned delivery
        Delivery returned = responseEntity.getBody();
        assertNull(returned);

        // Verify that no inner methods were called
////        verify(deliveryService, never()).updateEstimatedPrepTime(deliveryId, prepTime);
////        verify(deliveryService, never()).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdPrepPutUnauthorized() {
        // Mock data
        userType = UsersAuthenticationService.AccountType.INVALID;

        // Mock ratings and user type
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        // Call the method
        ResponseEntity<Delivery> responseEntity = sut.deliveriesDeliveryIdPrepPut(deliveryId, userId, prepTime);

        // Verify the response
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());

        // Verify the returned delivery
        Delivery returned = responseEntity.getBody();
        assertNull(returned);

        // Verify that no inner methods were called
//        verify(deliveryService, never()).updateEstimatedPrepTime(deliveryId, prepTime);
//        verify(deliveryService, never()).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingCourierPut(deliveryId, userId, 5);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(5, resultBody.getRatingCourier());

//        verify(deliveryService, times(1)).updateCourierRating(deliveryId, rating);
//        verify(deliveryService, times(2)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(userId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingCourierPut(deliveryId, userId, 0);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(0, resultBody.getRatingCourier());

//        verify(deliveryService, times(1)).updateCourierRating(deliveryId, rating);
//        verify(deliveryService, times(2)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(userId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
        Integer rating = 5;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingCourierPut(deliveryId, courierId, rating);

        assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(courierId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, 5);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(5, resultBody.getRatingRestaurant());

//        verify(deliveryService, times(1)).updateRestaurantRating(deliveryId, rating);
//        verify(deliveryService, times(2)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(userId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        Integer rating = 5;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, rating);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(userId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, 0);

        Delivery resultBody = result.getBody();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert resultBody != null;
        assertEquals(0, resultBody.getRatingRestaurant());

//        verify(deliveryService, times(1)).updateRestaurantRating(deliveryId, rating);
//        verify(deliveryService, times(2)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(userId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        Integer rating = 5;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Delivery> result = sut.deliveriesDeliveryIdRatingRestaurantPut(deliveryId, userId, rating);

        assertEquals(HttpStatus.valueOf(403), result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(userId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, restaurantId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNull(result.getBody());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(restaurantId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(restaurantId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, restaurantId);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(restaurantId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(customerId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingRestaurantGet(deliveryId, customerId);

        assertEquals(HttpStatus.OK, result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(customerId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingCourierGet(deliveryId, courierId);

        assertEquals(HttpStatus.OK, result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(courierId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(diffCourierId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);
        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingCourierGet(deliveryId, diffCourierId);

        //assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(diffCourierId);
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
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;

//        //Mock deliveryService/userRepo methods
        when(usersCommunication.getUserAccountType(customerId)).thenReturn(type);
//        when(deliveryService.getDelivery(deliveryId)).thenReturn(m);
        sut.insert(m);

        ResponseEntity<Integer> result = sut.deliveriesDeliveryIdRatingCourierGet(deliveryId, customerId);

        assertEquals(HttpStatus.OK, result.getStatusCode());

//        verify(deliveryService, times(1)).getDelivery(deliveryId);
        verify(usersCommunication, times(1)).getUserAccountType(customerId);
    }

    @Test
    void deliveriesAllAcceptedGetAdmin(){
        UUID deliveryId1 = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId1);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        UUID deliveryId2 = UUID.randomUUID();
        Delivery m2 = new Delivery();
        m2.setDeliveryID(deliveryId2);
        m2.setCourierID(courierId);
        m2.setRestaurantID(vendorId);
        m2.setCustomerID(customerId);
        sut.insert(m2);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<List<Delivery>> result = sut.deliveriesAllAcceptedGet(userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert(Objects.requireNonNull(result.getBody()).contains(m1));
        assert(!result.getBody().contains(m2));

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesAllAcceptedGetCourier(){
        UUID deliveryId1 = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId1);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        UUID deliveryId2 = UUID.randomUUID();
        Delivery m2 = new Delivery();
        m2.setDeliveryID(deliveryId2);
        m2.setCourierID(courierId);
        m2.setRestaurantID(vendorId);
        m2.setCustomerID(customerId);
        sut.insert(m2);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<List<Delivery>> result = sut.deliveriesAllAcceptedGet(userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assert(Objects.requireNonNull(result.getBody()).contains(m1));
        assert(!result.getBody().contains(m2));

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesAllAcceptedGetCustomer(){
        UUID deliveryId1 = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId1);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<List<Delivery>> result = sut.deliveriesAllAcceptedGet(userId);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesAllAcceptedGetVendor(){
        UUID deliveryId1 = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId1);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<List<Delivery>> result = sut.deliveriesAllAcceptedGet(userId);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesAllAcceptedGetInExistent(){
        UUID deliveryId1 = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId1);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;

        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<List<Delivery>> result = sut.deliveriesAllAcceptedGet(userId);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());

        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdCourierGetAdmin(){
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdCourierGet(deliveryId, userId);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(res.getBody(), courierId);
        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdCourierGetVendor(){
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(type);

        // Vendor checks the courier for their own order
        ResponseEntity<String> res1 = sut.deliveriesDeliveryIdCourierGet(deliveryId, vendorId);
        assertEquals(HttpStatus.OK, res1.getStatusCode());
        verify(usersCommunication, times(1)).getUserAccountType(vendorId);
        assertEquals(res1.getBody(), courierId);

        // Vendor is not allowed to check the courier of another vendor's order
        ResponseEntity<String> res2 = sut.deliveriesDeliveryIdCourierGet(deliveryId, userId);
        assertEquals(HttpStatus.FORBIDDEN, res2.getStatusCode());
        verify(usersCommunication, times(1)).getUserAccountType(userId);
        assertEquals(res2.getBody(), "User lacks necessary permissions.");
    }

    @Test
    void deliveriesDeliveryIdCourierGetCustomer(){
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdCourierGet(deliveryId, userId);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        assertEquals(res.getBody(), "User lacks necessary permissions.");
        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdCourierGetInExistent(){
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdCourierGet(deliveryId, userId);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertEquals(res.getBody(), "User lacks valid authentication credentials.");
        verify(usersCommunication, times(1)).getUserAccountType(userId);
    }

    @Test
    void deliveriesDeliveryIdCourierGetCourier(){
        String courierId = "courier@testmail.com";
        String otherCourierId = "othercourier@testmail.com";

        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        UUID deliveryId2 = UUID.randomUUID();
        Delivery m2 = new Delivery();
        m2.setDeliveryID(deliveryId2);
        m2.setCourierID(courierId);
        m2.setRestaurantID(vendorId);
        m2.setCustomerID(customerId);
        sut.insert(m2);

        UUID deliveryId3 = UUID.randomUUID();
        Delivery m3 = new Delivery();
        m3.setDeliveryID(deliveryId3);
        m3.setCourierID(otherCourierId);
        m3.setRestaurantID(vendorId);
        m3.setCustomerID(customerId);
        sut.insert(m3);

        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(type);

        ResponseEntity<String> res1 = sut.deliveriesDeliveryIdCourierGet(deliveryId, courierId);
        assertEquals(HttpStatus.NOT_FOUND, res1.getStatusCode());
        assertEquals(res1.getBody(), "No courier assigned to order.");
        verify(usersCommunication, times(1)).getUserAccountType(courierId);

        ResponseEntity<String> res2 = sut.deliveriesDeliveryIdCourierGet(deliveryId2, courierId);
        assertEquals(HttpStatus.OK, res2.getStatusCode());
        assertEquals(res2.getBody(), courierId);
        verify(usersCommunication, times(2)).getUserAccountType(courierId);

        ResponseEntity<String> res3 = sut.deliveriesDeliveryIdCourierGet(deliveryId3, courierId);
        assertEquals(HttpStatus.FORBIDDEN, res3.getStatusCode());
        assertEquals(res3.getBody(), "User lacks necessary permissions.");
        verify(usersCommunication, times(3)).getUserAccountType(courierId);
    }

    @Test
    void deliveriesDeliveryIdCourierPutNonCourier(){
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        ResponseEntity<Delivery> res = sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertNull(res.getBody());
        verify(usersCommunication, times(2)).getUserAccountType(any());
    }

    @Test
    void deliveriesDeliveryIdCourierPutCustomer(){
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.CLIENT;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);

        ResponseEntity<Delivery> res = sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        assertNull(res.getBody());
        verify(usersCommunication, times(2)).getUserAccountType(any());
    }

    @Test
    void deliveriesDeliveryIdCourierPutInExistent(){
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.INVALID;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);

        ResponseEntity<Delivery> res = sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertNull(res.getBody());
        verify(usersCommunication, times(2)).getUserAccountType(any());
    }

    @Test
    void deliveriesDeliveryIdCourierPutAdmin(){
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier@testmail.com";
        String otherCourierId = "otherCourier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(courierId);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.ADMIN;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(otherCourierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);

        ResponseEntity<Delivery> res = sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, otherCourierId);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(Objects.requireNonNull(res.getBody()).getCourierID(), otherCourierId);
        verify(usersCommunication, times(2)).getUserAccountType(any());
    }

    @Test
    void deliveriesDeliveryIdCourierPutCourier(){
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String otherCourierId = "otherCourier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        UUID deliveryId2 = UUID.randomUUID();
        Delivery m2 = new Delivery();
        m2.setDeliveryID(deliveryId2);
        m2.setCourierID(otherCourierId);
        m2.setRestaurantID(vendorId);
        m2.setCustomerID(customerId);
        sut.insert(m2);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.COURIER;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);

        ResponseEntity<Delivery> res = sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, userId);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(Objects.requireNonNull(res.getBody()).getCourierID(), userId);
        verify(usersCommunication, times(2)).getUserAccountType(any());

        ResponseEntity<Delivery> res2 = sut.deliveriesDeliveryIdCourierPut(deliveryId2, userId, userId);
        assertEquals(HttpStatus.FORBIDDEN, res2.getStatusCode());
        verify(usersCommunication, times(4)).getUserAccountType(any());
    }

    @Test
    void deliveriesDeliveryIdCourierPutVendor(){
        UUID deliveryId = UUID.randomUUID();
        String customerId = "customer@testmail.com";
        String vendorId = "vendor@testmail.com";
        String courierId = "courier7@testmail.com";
        String otherCourierId = "otherCourier@testmail.com";
        Delivery m1 = new Delivery();
        m1.setDeliveryID(deliveryId);
        m1.setCourierID(null);
        m1.setRestaurantID(vendorId);
        m1.setCustomerID(customerId);
        sut.insert(m1);

        UUID deliveryId2 = UUID.randomUUID();
        Delivery m2 = new Delivery();
        m2.setDeliveryID(deliveryId2);
        m2.setCourierID(courierId);
        m2.setRestaurantID(vendorId);
        m2.setCustomerID(customerId);
        sut.insert(m2);

        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(vendorId);
        restaurant.setLocation(List.of(95.5,56.5));
        String courierInList = "courier1@testmail.com";
        restaurant.setCouriers(List.of(courierInList));
        repo2.save(restaurant);

        String userId = "user@testmail.com";
        UsersAuthenticationService.AccountType type = UsersAuthenticationService.AccountType.VENDOR;
        when(usersCommunication.getUserAccountType(userId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(vendorId)).thenReturn(type);
        when(usersCommunication.getUserAccountType(courierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(otherCourierId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(courierInList)).thenReturn(UsersAuthenticationService.AccountType.COURIER);

        //Assign courier to different vendor
        ResponseEntity<Delivery> res = sut.deliveriesDeliveryIdCourierPut(deliveryId, userId, courierId);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        verify(usersCommunication, times(2)).getUserAccountType(any());

        //Assign different courier to its order
        ResponseEntity<Delivery> res2 = sut.deliveriesDeliveryIdCourierPut(deliveryId2, vendorId, otherCourierId);
        assertEquals(HttpStatus.FORBIDDEN, res2.getStatusCode());
        // verify(usersCommunication, times(4)).getUserAccountType(any());

        //Courier not in the list
        ResponseEntity<Delivery> res3 = sut.deliveriesDeliveryIdCourierPut(deliveryId, vendorId, courierId);
        assertEquals(HttpStatus.FORBIDDEN, res3.getStatusCode());
        //verify(usersCommunication, times(6)).getUserAccountType(any());

        ResponseEntity<Delivery> res4 = sut.deliveriesDeliveryIdCourierPut(deliveryId, vendorId, courierInList);
        assertEquals(HttpStatus.OK, res4.getStatusCode());
        assertEquals(Objects.requireNonNull(res4.getBody()).getCourierID(), courierInList);
        //verify(usersCommunication, times(8)).getUserAccountType(any());
    }


    @Test
    void deliveriesPostNull() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(null));
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

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> sut.deliveriesPost(dpr));
        assertEquals(exception.getMessage(), "Unexpected value 'INVALID'");
    }

    @Test
    void deliveriesPostEmptyEmail() {
        DeliveriesPostRequest dpr = new DeliveriesPostRequest();
        dpr.setStatus("pending");
        dpr.setCustomerId("user@testmail.com");
        dpr.setOrderId(UUID.randomUUID().toString());
        dpr.setDeliveryAddress(List.of(50.4, 32.6));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(dpr));
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

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(dpr));
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

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(dpr));
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

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(dpr));
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

        repo2.save(r);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesPost(dpr));
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

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
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

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
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

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
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

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
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

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
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

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
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

        repo2.save(r);
        ResponseEntity<Delivery> result = sut.deliveriesPost(dpr);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Objects.requireNonNull(result.getBody()).getDeliveryID().toString(), orderId);
    }
    @Test
    void getUnexpectedEventBadRequest() {
        UUID del_id = UUID.randomUUID();
        ResponseEntity<nl.tudelft.sem.template.model.Error> res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, null);
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
        res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, "");
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getUnexpectedEventNotFound() {
        UUID del_id = UUID.randomUUID();
        String courierID = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, courierID));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCourierID(courierID);

        sut.insert(delivery);
        ResponseEntity<nl.tudelft.sem.template.model.Error> res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, courierID);
        assertEquals(res.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void getUnexpectedEventUnauthorized() {
        UUID del_id = UUID.randomUUID();
        nl.tudelft.sem.template.model.Error e = new nl.tudelft.sem.template.model.Error();
        e.setType(ErrorType.NONE);
        String cID = "ehi_im_a_user@gmail.com";
        String coID = "ehi_im_a_courier@gmail.com";
        String vID = "ehi_im_a_user@gmail.com";
        String invalid = "ehi_im_an_impostor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setError(e);
        delivery.setCustomerID(cID + "h");
        delivery.setCourierID(coID + "h");
        delivery.setRestaurantID(vID + "h");

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(invalid)).thenReturn(UsersAuthenticationService.AccountType.INVALID);


        ResponseEntity<nl.tudelft.sem.template.model.Error> res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, invalid);
        assertEquals(res.getStatusCode(), HttpStatus.UNAUTHORIZED);
    }

//    @Test
//    void getUnexpectedEventAuthorized() {
//        String admin = "hi_im_an_admin@gmail.com";
//        UUID del_id = UUID.randomUUID();
//        String cID = "hi_im_a_user@gmail.com";
//        String coID = "hi_im_a_courier@gmail.com";
//        String vID = "hi_im_a_vendor@gmail.com";
//        Error e = new Error();
//        e.setType(ErrorType.NONE);
//        Delivery delivery = new Delivery();
//        delivery.setDeliveryID(del_id);
//        delivery.setCustomerID(cID);
//        delivery.setCourierID(coID);
//        delivery.setRestaurantID(vID);
//        delivery.setError(e);
//
//        sut.insert(delivery);
//
//        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(true);
//        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(true);
//        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(true);
//        when(usersCommunication.checkUserAccessToDelivery(admin, delivery)).thenReturn(true);
//
//        ResponseEntity<Error> res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, coID);
//        assertEquals(res.getStatusCode(), HttpStatus.OK);
//        assertNotNull(res.getBody());
//        assertEquals(res.getBody().getType(), ErrorType.NONE);
//
//        res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, vID);
//        assertEquals(res.getStatusCode(), HttpStatus.OK);
//        assertNotNull(res.getBody());
//        assertEquals(res.getBody().getType(), ErrorType.NONE);
//
//        res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, admin);
//        assertEquals(res.getStatusCode(), HttpStatus.OK);
//        assertNotNull(res.getBody());
//        assertEquals(res.getBody().getType(), ErrorType.NONE);
//
//        res = sut.deliveriesDeliveryIdUnexpectedEventGet(del_id, cID);
//        assertEquals(res.getStatusCode(), HttpStatus.OK);
//        assertNotNull(res.getBody());
//        assertEquals(res.getBody().getType(), ErrorType.NONE);
//    }

    @Test
    void getRestaurantBadRequest() {
        UUID del_id = UUID.randomUUID();
        ResponseEntity<String> res = sut.deliveriesDeliveryIdRestaurantGet(del_id, null);
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
        res = sut.deliveriesDeliveryIdRestaurantGet(del_id, "");
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getRestaurantNotFound() {
        UUID del_id = UUID.randomUUID();
        String courierID = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, courierID));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCourierID(courierID);

        sut.insert(delivery);
        ResponseEntity<String> res = sut.deliveriesDeliveryIdRestaurantGet(del_id, courierID);
        assertEquals(res.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void getRestaurantUnauthorized() {
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_user@gmail.com";
        String invalid = "hi_im_an_impostor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID + "h");
        delivery.setCourierID(coID + "h");
        delivery.setRestaurantID(vID + "h");

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(invalid)).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(invalid, delivery)).thenReturn(false);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdRestaurantGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdRestaurantGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdRestaurantGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdRestaurantGet(del_id, invalid);
        assertEquals(res.getStatusCode(), HttpStatus.UNAUTHORIZED);
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

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(admin)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(admin, delivery)).thenReturn(true);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdRestaurantGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), vID);

        res = sut.deliveriesDeliveryIdRestaurantGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), vID);

        res = sut.deliveriesDeliveryIdRestaurantGet(del_id, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), vID);

        res = sut.deliveriesDeliveryIdRestaurantGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void getDeliveredTimeBadRequest() {
        UUID del_id = UUID.randomUUID();
        ResponseEntity<OffsetDateTime> res = sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, null);
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
        res = sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, "");
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getDeliveredTimeNotFound() {
        UUID del_id = UUID.randomUUID();
        String customerID = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, customerID));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(customerID);

        sut.insert(delivery);
        ResponseEntity<OffsetDateTime> res = sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, customerID);
        assertEquals(res.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void getDeliveredTimeUnauthorized() {
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_user@gmail.com";
        String invalid = "hi_im_an_impostor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID + "h");
        delivery.setCourierID(coID + "h");
        delivery.setRestaurantID(vID + "h");

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(invalid)).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(invalid, delivery)).thenReturn(false);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdCustomerGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdCustomerGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdCustomerGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdCustomerGet(del_id, invalid);
        assertEquals(res.getStatusCode(), HttpStatus.UNAUTHORIZED);
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

        sut.insert(delivery);

        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(admin, delivery)).thenReturn(true);

        ResponseEntity<OffsetDateTime> res = sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), t);

        res = sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), t);

        res = sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), t);

        res = sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, cID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
    }

    @Test
    void getCustomerBadRequest() {
        UUID del_id = UUID.randomUUID();
        ResponseEntity<String> res = sut.deliveriesDeliveryIdCustomerGet(del_id, null);
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
        res = sut.deliveriesDeliveryIdCustomerGet(del_id, "");
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getCustomerDeliveryNotFound() {
        UUID del_id = UUID.randomUUID();
        String customerID = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut.deliveriesDeliveryIdDeliveredTimeGet(del_id, customerID));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(customerID);

        sut.insert(delivery);
        ResponseEntity<String> res = sut.deliveriesDeliveryIdCustomerGet(del_id, customerID);
        assertEquals(res.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void getCustomerDeliveryUnauthorized() {
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_user@gmail.com";
        String invalid = "hi_im_an_impostor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID + "h");
        delivery.setCourierID(coID + "h");
        delivery.setRestaurantID(vID + "h");

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(invalid)).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(invalid, delivery)).thenReturn(false);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdCustomerGet(del_id, cID);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());

        res = sut.deliveriesDeliveryIdCustomerGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdCustomerGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdCustomerGet(del_id, invalid);
        assertEquals(res.getStatusCode(), HttpStatus.UNAUTHORIZED);
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

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(admin)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(admin, delivery)).thenReturn(true);

        ResponseEntity<String> res = sut.deliveriesDeliveryIdCustomerGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), cID);

        res = sut.deliveriesDeliveryIdCustomerGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), cID);

        res = sut.deliveriesDeliveryIdCustomerGet(del_id, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), cID);

        res = sut.deliveriesDeliveryIdCustomerGet(del_id, cID);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    void getCurrentLocationBadRequest() {
        UUID del_id = UUID.randomUUID();
        ResponseEntity<List<Double>> res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, null);
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
        res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, "");
        assertEquals(res.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void getCurrentLocationDeliveryNotFound() {
        UUID del_id = UUID.randomUUID();
        String customerID = "hi_im_a_user@gmail.com";
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut.deliveriesDeliveryIdCurrentLocationGet(del_id, customerID));

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(customerID);

        sut.insert(delivery);
        ResponseEntity<List<Double>> res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, customerID);
        assertEquals(res.getStatusCode(), HttpStatus.NOT_FOUND);
    }
    //deliveriesDeliveryIdCurrentLocationGet
    @Test
    void getCurrentLocationUnauthorized() {
        UUID del_id = UUID.randomUUID();
        String cID = "hi_im_a_user@gmail.com";
        String coID = "hi_im_a_courier@gmail.com";
        String vID = "hi_im_a_user@gmail.com";
        String invalid = "hi_im_an_impostor@gmail.com";
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(del_id);
        delivery.setCustomerID(cID + "h");
        delivery.setCourierID(coID + "h");
        delivery.setRestaurantID(vID + "h");
        delivery.setCurrentLocation(coords);

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(invalid)).thenReturn(UsersAuthenticationService.AccountType.INVALID);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(invalid, delivery)).thenReturn(false);

        ResponseEntity<List<Double>> res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, cID);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());

        res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.FORBIDDEN);

        res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, invalid);
        assertEquals(res.getStatusCode(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCurrentLocationAuthorized() {
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
        delivery.setCurrentLocation(coords);

        sut.insert(delivery);

        when(usersCommunication.getUserAccountType(cID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        when(usersCommunication.getUserAccountType(coID)).thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersCommunication.getUserAccountType(vID)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.getUserAccountType(admin)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersCommunication.checkUserAccessToDelivery(cID, delivery)).thenReturn(false);
        when(usersCommunication.checkUserAccessToDelivery(coID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(vID, delivery)).thenReturn(true);
        when(usersCommunication.checkUserAccessToDelivery(admin, delivery)).thenReturn(true);

        ResponseEntity<List<Double>> res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, coID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), coords);

        res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, vID);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), coords);

        res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, admin);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
        assertEquals(res.getBody(), coords);

        res = sut.deliveriesDeliveryIdCurrentLocationGet(del_id, cID);
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
        restaurantController.insert(restaurant);
        sut.insert(delivery);

        when(usersCommunication.checkUserAccessToDelivery(customerID, delivery)).thenReturn(true);
        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        List<Double> deliveryAddress = sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), customerID).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        assertThat(sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(),customerID).getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    void pickup_get_not_found() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        String userId = "user@user.com";
        delivery.setCustomerID(userId);
        sut.insert(delivery);
        //when(usersCommunication.checkUserAccessToDelivery(userId, delivery)).thenReturn(true);
        //when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        UUID invalidDeliveryId = UUID.randomUUID();
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut.deliveriesDeliveryIdPickupLocationGet(invalidDeliveryId, userId));
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
        restaurantController.insert(restaurant);
        sut.insert(delivery);

        when(usersCommunication.checkUserAccessToDelivery(customerID, delivery)).thenReturn(true);
        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        OffsetDateTime pickupTime = sut.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), customerID).getBody();
        assertThat(pickupTime).isEqualTo(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
        assertThat(sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), customerID).getStatusCode()).isEqualTo(HttpStatus.OK);

    }

//    @Test
//    void pickup_time_put() {
//        UUID deliveryId = UUID.randomUUID();
//        Delivery delivery = new Delivery();
//        delivery.setDeliveryID(deliveryId);
//        String customerID = "user@user.com";
//        delivery.setCustomerID(customerID);
//        String restaurantId = "pizzahut@yahoo.com";
//        Restaurant restaurant = new Restaurant();
//        restaurant.setRestaurantID(restaurantId);
//        restaurant.setLocation(new ArrayList<>(Arrays.asList(100.0, 100.0)));
//        delivery.setPickupTime(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
//        delivery.setRestaurantID(restaurantId);
//        restaurantController.insert(restaurant);
//        sut.insert(delivery);
//
//        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);
//        when(usersCommunication.checkUserAccessToDelivery(customerID, delivery)).thenReturn(true);
//        OffsetDateTime pickupTime;
//        pickupTime = sut.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), customerID).getBody();
//        assertThat(pickupTime).isEqualTo(OffsetDateTime.parse("2021-09-30T15:30:00+01:00"));
//        assertThat(sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), customerID).getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        sut.deliveriesDeliveryIdPickupPut(delivery.getDeliveryID(), customerID, OffsetDateTime.parse("2022-09-30T15:30:00+01:00"));
//        pickupTime = sut.deliveriesDeliveryIdPickupGet(delivery.getDeliveryID(), customerID).getBody();
//        assertThat(pickupTime).isEqualTo(OffsetDateTime.parse("2022-09-30T15:30:00+01:00"));
//        assertThat(sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), customerID).getStatusCode()).isEqualTo(HttpStatus.OK);
//
//
//    }

    @Test
    void address_get_found() {
        Delivery delivery = new Delivery();
        UUID deliveryID = UUID.randomUUID();
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setDeliveryID(deliveryID);
        String userId = "user@user.com";
        delivery.setCustomerID(userId);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        List<Double> deliveryAddress = sut.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), userId).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        assertThat(sut.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), userId).getStatusCode()).isEqualTo(HttpStatus.OK);

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
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        List<Double> deliveryAddress = sut.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), userId).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(List.of()));
        assertThat(sut.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), userId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

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
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);
        List<Double> deliveryAddress = sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), userId).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(List.of()));
        assertThat(sut.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), userId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    }

    @Test
    void address_get_notFound() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        sut.insert(delivery);
        UUID invalidDeliveryId = UUID.randomUUID();
        assertThrows(DeliveryService.DeliveryNotFoundException.class, () -> sut.deliveriesDeliveryIdDeliveryAddressGet(invalidDeliveryId, null));
    }
    @Test
    void getDeliveryAuthorized() {
        UUID deliveryUUID = UUID.randomUUID();
        String customerID = "test@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryUUID);
        delivery.setRestaurantID(customerID);
        delivery.setCourierID(customerID);
        delivery.setCustomerID(customerID);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        ResponseEntity<Delivery> res = sut.deliveriesDeliveryIdGet(deliveryUUID, customerID);

        assertEquals(res.getBody(),delivery);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
    }
    @Test
    void getDeliveryForbidden() {
        UUID deliveryUUID = UUID.randomUUID();
        String customerID = "test@test.com";
        String newCustomerID = "newtest@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryUUID);
        delivery.setRestaurantID(customerID);
        delivery.setCourierID(customerID);
        delivery.setCustomerID(newCustomerID);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesDeliveryIdGet(deliveryUUID, customerID));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "THIS ACTION IS FORBIDDEN");
    }
    @Test
    void getDeliveryUnauthorized() {
        UUID deliveryUUID = UUID.randomUUID();
        String customerID = "test@test.com";
        String newCustomerID = "newtest@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryUUID);
        delivery.setRestaurantID(customerID);
        delivery.setCourierID(customerID);
        delivery.setCustomerID(newCustomerID);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.fromValue("UNAUTHORIZED"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesDeliveryIdGet(deliveryUUID, customerID));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals(exception.getReason(), "YOU ARE NOT AUTHORIZED");
    }

    @Test
    void getDeliveryPrepTimeAuthorized() {
        UUID deliveryUUID = UUID.randomUUID();
        String customerID = "test@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryUUID);
        delivery.setRestaurantID(customerID);
        delivery.setEstimatedPrepTime(30);
        delivery.setCourierID(customerID);
        delivery.setCustomerID(customerID);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        ResponseEntity<Integer> res = sut.deliveriesDeliveryIdPrepGet(deliveryUUID, customerID);

        assertEquals(res.getBody(),30);
        assertEquals(res.getStatusCode(), HttpStatus.OK);
    }
    @Test
    void getDeliveryPrepTimeForbidden() {
        UUID deliveryUUID = UUID.randomUUID();
        String customerID = "test@test.com";
        String newCustomerID = "newtest@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryUUID);
        delivery.setRestaurantID(customerID);
        delivery.setCourierID(customerID);
        delivery.setEstimatedPrepTime(30);
        delivery.setCustomerID(newCustomerID);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.CLIENT);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesDeliveryIdPrepGet(deliveryUUID, customerID));
        assertEquals(exception.getStatus(), HttpStatus.FORBIDDEN);
        assertEquals(exception.getReason(), "THIS ACTION IS FORBIDDEN");
    }
    @Test
    void getDeliveryPrepTimeUnauthorized() {
        UUID deliveryUUID = UUID.randomUUID();
        String customerID = "test@test.com";
        String newCustomerID = "newtest@test.com";

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryUUID);
        delivery.setRestaurantID(customerID);
        delivery.setCourierID(customerID);
        delivery.setCustomerID(newCustomerID);
        delivery.setEstimatedPrepTime(30);
        sut.insert(delivery);
        when(usersCommunication.getUserAccountType(customerID)).thenReturn(UsersAuthenticationService.AccountType.fromValue("UNAUTHORIZED"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> sut.deliveriesDeliveryIdPrepGet(deliveryUUID, customerID));
        assertEquals(exception.getStatus(), HttpStatus.UNAUTHORIZED);
        assertEquals(exception.getReason(), "YOU ARE NOT AUTHORIZED");
    }

}