package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.transaction.Transactional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
class ErrorServiceTest {
    @Autowired
    private DeliveryRepository dr;
    @Autowired
    private ErrorRepository er;
    @Autowired
    private RestaurantRepository rr;
    private ErrorService sut;
    private DeliveryService deliveryService;


    @BeforeEach
    void setUp() {
        sut = new ErrorService(er, dr);
        deliveryService = new DeliveryService(dr, new GPS(), rr);
    }

    @Test
    void getErrorNotFound() {
        UUID deliveryId = UUID.randomUUID();
        assertThrows(ErrorService.ErrorNotFoundException.class, () -> sut.getError(deliveryId));

    }

    @Test
    void getError() {
        UUID deliveryId = UUID.randomUUID();
        Error error = new Error();
        error.setErrorId(deliveryId);
        error.setReason("Some compelling reason to get no food.");
        error.setType(ErrorType.OTHER);
        Error test = sut.insert(error);
        assertEquals(error, test);
        assertEquals(error, sut.getError(deliveryId));
    }

    @Test
    void updateErrorDelivered() {
        // Set Data
        UUID deliveryId = UUID.randomUUID();
        Error error = new Error();
        error.setErrorId(deliveryId);
        error.setType(ErrorType.OTHER);
        error.setReason("Some compelling reason to get no food.");

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setError(error);
        // Persist in repositories
        sut.insert(error);
        deliveryService.insert(delivery);

        // Create updated entity
        UUID updateId = UUID.randomUUID();
        String updateReason = "Even more compelling reason not to get food.";
        ErrorType updateType = ErrorType.CANCELLED;
        Integer updateValue = Integer.MAX_VALUE;
        Error updateError = new Error();
        updateError.setErrorId(updateId);
        updateError.setReason(updateReason);
        updateError.setType(updateType);
        updateError.setValue(updateValue);

        // Act and Assert
        assertThrows(DeliveryService.OrderAlreadyDeliveredException.class, () -> sut.updateError(deliveryId, updateError));
    }

    @Test
    void updateError() {
        // Set Data
        UUID deliveryId = UUID.randomUUID();
        Error error = new Error();
        error.setErrorId(deliveryId);
        error.setType(ErrorType.OTHER);
        error.setReason("Some compelling reason to get no food.");

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setStatus(DeliveryStatus.ACCEPTED);
        delivery.setError(error);

        // Persist in repositories
        sut.insert(error);
        deliveryService.insert(delivery);

        // Create updated entity
        UUID updateId = UUID.randomUUID();
        String updateReason = "Even more compelling reason not to get food.";
        ErrorType updateType = ErrorType.CANCELLED;
        Integer updateValue = Integer.MAX_VALUE;
        Error updateError = new Error();
        updateError.setErrorId(updateId);
        updateError.setReason(updateReason);
        updateError.setType(updateType);
        updateError.setValue(updateValue);
        // Act
        Error result = sut.updateError(deliveryId, updateError);

        // Assert
        assertEquals(deliveryId, result.getErrorId());
        assertEquals(updateReason, result.getReason());
        assertEquals(updateType, result.getType());
        assertEquals(updateValue, result.getValue());
    }

    @Test
    void insertThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> sut.insert(null));
    }
}