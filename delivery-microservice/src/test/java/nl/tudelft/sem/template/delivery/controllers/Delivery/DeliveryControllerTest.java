package nl.tudelft.sem.template.delivery.controllers.Delivery;

import java.util.Objects;

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
import nl.tudelft.sem.template.model.DeliveriesPostRequest;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.ErrorType;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    private UsersAuthenticationService usersCommunication;

    private DeliveryController sut;

    private TestDeliveryRepository repo1;

    private TestRestaurantRepository repo2;
    private RestaurantController restaurantController;

    String userId;
    UsersAuthenticationService.AccountType userType;
    UUID deliveryId;
    Integer prepTime;
    Delivery delivery;

    @BeforeEach
    void setUp() {
        userId = "user@example.org";
        deliveryId = UUID.randomUUID();
        prepTime = 25;
        delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setEstimatedPrepTime(prepTime);
        repo2 = new TestRestaurantRepository();
        usersCommunication = mock(UsersAuthenticationService.class);
        repo1 = new TestDeliveryRepository();
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
}