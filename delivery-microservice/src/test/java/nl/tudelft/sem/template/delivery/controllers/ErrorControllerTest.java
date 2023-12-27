package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestErrorRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.ErrorService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErrorControllerTest {

    private UsersCommunication usersCommunication;
    private ErrorController sut;
    private DeliveryController deliveryController;

    String userId, userType, reason;
    UUID deliveryId;
    Integer value;
    ErrorType errorType;
    Delivery delivery;
    Error error;

    UUID updateID;
    ErrorType updateType;
    String updateReason;
    Integer updateValue;
    Error updateError;

    @BeforeEach
    void setUp() {
        // Fields
        userId = "user@example.org";
        deliveryId = UUID.randomUUID();

        // Entities
        error = new Error();
        delivery = new Delivery();
        error.setErrorId(deliveryId);
        error.setDelivery(delivery);
        delivery.setDeliveryID(deliveryId);
        delivery.setError(error);

        // Update Entity
        updateID = UUID.randomUUID();
        updateType = ErrorType.OTHER;
        updateReason = "Some more compelling updated reason!";
        updateValue = null;
        updateError = new Error();
        updateError.setType(updateType);
        updateError.setReason(updateReason);
        updateError.setValue(updateValue);

        // Repositories
        TestErrorRepository repo1 = new TestErrorRepository();
        TestDeliveryRepository repo2 = new TestDeliveryRepository();
        TestRestaurantRepository repo3 = new TestRestaurantRepository();

        // Services and dependency injections
        DeliveryService deliveryService = new DeliveryService(repo2, repo3);
        ErrorService errorService = new ErrorService(repo1, repo2);
        usersCommunication = mock(UsersCommunication.class);

        // Controllers
        sut = new ErrorController(errorService, deliveryService, usersCommunication);
        deliveryController = new DeliveryController(deliveryService, usersCommunication, null);
    }

    @Test
    void errorsDeliveryIdGetAdmin() {
        // Set Data
        userType = "admin";
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(userId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(errorType, getError.getType());
        assertEquals(reason, getError.getReason());
        assertEquals(value, getError.getValue());
    }

    @Test
    void errorsDeliveryIdGetVendor() {
        // Set Data
        userType = "vendor";
        errorType = ErrorType.DELAYED;
        reason = "Due to busy schedule, order will be 20 minutes late.";
        value = 20;
        error.setType(errorType);
        error.setReason(reason);
        error.setValue(value);
        delivery.setRestaurantID(userId);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(userId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(errorType, getError.getType());
        assertEquals(reason, getError.getReason());
        assertEquals(value, getError.getValue());
    }

    @Test
    void errorsDeliveryIdGetCourier() {
        // Set Data
        userType = "courier";
        errorType = ErrorType.NONE;
        error.setType(errorType);
        delivery.setCourierID(userId);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(userId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(errorType, getError.getType());
        assertEquals(reason, getError.getReason());
        assertEquals(value, getError.getValue());
    }

    @Test
    void errorsDeliveryIdGetCustomer() {
        // Set Data
        userType = "customer";
        errorType = ErrorType.OTHER;
        reason = "Payment was unsuccessful. Customer intervention expected.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setCustomerID(userId);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(userId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(errorType, getError.getType());
        assertEquals(reason, getError.getReason());
        assertEquals(value, getError.getValue());
    }

    @Test
    void errorsDeliveryIdGetCustomerForbidden() {
        // Set Data
        String otherUserId = "other@user.org";
        userType = "customer";
        errorType = ErrorType.OTHER;
        reason = "Payment was unsuccessful. Customer intervention expected.";
        error.setType(errorType);
        error.setReason(reason);
        delivery.setCustomerID(otherUserId);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(userId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertNull(getError);
    }

    @Test
    void errorsDeliveryIdGetUnauthorized() {
        // Set Data
        userType = "non-existent";
        errorType = ErrorType.OTHER;
        reason = "Payment was unsuccessful. Customer intervention expected.";
        error.setType(errorType);
        error.setReason(reason);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(userId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertNull(getError);
    }

    @Test
    void errorsDeliveryIdGetNoUserID() {
        // Set Data
        String emptyUserId = "";
        userType = "admin";
        errorType = ErrorType.OTHER;
        reason = "Payment was unsuccessful. Customer intervention expected.";
        error.setType(errorType);
        error.setReason(reason);
        when(usersCommunication.getAccountType(any(String.class))).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdGet(emptyUserId, deliveryId);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, never()).getAccountType(any(String.class));
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNull(getError);
    }

    @Test
    void errorsDeliveryIdPutAdmin() {
        // Set Data
        userType = "admin";
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdPut(userId, deliveryId, updateError);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(updateType, getError.getType());
        assertEquals(updateReason, getError.getReason());
        assertEquals(updateValue, getError.getValue());
    }

    @Test
    void errorsDeliveryIdPutVendor() {
        // Set Data
        userType = "vendor";
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setRestaurantID(userId);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdPut(userId, deliveryId, updateError);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(updateType, getError.getType());
        assertEquals(updateReason, getError.getReason());
        assertEquals(updateValue, getError.getValue());
    }

    @Test
    void errorsDeliveryIdPutCourier() {
        // Set Data
        userType = "courier";
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setCourierID(userId);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdPut(userId, deliveryId, updateError);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(getError);
        assertEquals(deliveryId, getError.getErrorId());
        assertEquals(updateType, getError.getType());
        assertEquals(updateReason, getError.getReason());
        assertEquals(updateValue, getError.getValue());
    }

    @Test
    void errorsDeliveryIdPutCustomer() {
        // Set Data
        userType = "customer";
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setCourierID(userId);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdPut(userId, deliveryId, updateError);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertNull(getError);
    }

    @Test
    void errorsDeliveryIdPutUnauthorized() {
        // Set Data
        userType = "non-existent";
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setCourierID(userId);
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdPut(userId, deliveryId, updateError);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, times(1)).getAccountType(userId);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertNull(getError);
    }

    @Test
    void errorsDeliveryIdPutNoUserId() {
        // Set Data
        String emptyUserId = "";
        userType = "customer";
        errorType = ErrorType.CANCELLED;
        reason = "Food ingredients are not currently available to prepare your order. Apologies for the inconvenience.";
        value = null;
        error.setType(errorType);
        error.setReason(reason);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setCourierID(userId);
        when(usersCommunication.getAccountType(any(String.class))).thenReturn(userType);

        // Persist in Test Repos
        sut.insert(error);
        deliveryController.insert(delivery);

        // Act
        ResponseEntity<Error> result = sut.errorsDeliveryIdPut(emptyUserId, deliveryId, updateError);
        Error getError = result.getBody();

        // Assert
        verify(usersCommunication, never()).getAccountType(any(String.class));
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNull(getError);
    }

    @Test
    void isWhiteSpace() {
        assertTrue(sut.isNullOrEmpty(" "));
    }

    @Test
    void isEmpty() {
        assertTrue(sut.isNullOrEmpty(""));
    }

    @Test
    void isNull() {
        assertTrue(sut.isNullOrEmpty(null));
    }

    @Test
    void insertThrowsIllegalArgument() {
        ResponseEntity<Void> result = sut.insert(null);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }
}