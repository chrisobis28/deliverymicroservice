package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.util.UUID;


@Service
public class ErrorService {

    private final ErrorRepository errorRepository;
    private final DeliveryRepository deliveryRepository;

    /**
     * Constructor
     *
     * @param errorRepository    the repository storing information about unexpected events
     * @param deliveryRepository the repository storing delivery information
     */
    @Autowired
    public ErrorService(ErrorRepository errorRepository, DeliveryRepository deliveryRepository) {
        this.errorRepository = errorRepository;
        this.deliveryRepository = deliveryRepository;
    }

    /**
     * Gets the error of a given delivery (one to one correspondence)
     *
     * @param deliveryId ID of the given delivery
     * @return the error of the delivery
     */
    public Error getError(UUID deliveryId) {
        return errorRepository.findById(deliveryId)
                .orElseThrow(ErrorService.ErrorNotFoundException::new);
    }

    /**
     * Updates the error of a given delivery (one to one correspondence)
     *
     * @param deliveryId ID of the given delivery
     * @param error      updated error item
     * @return the error of the delivery
     */
    @Transactional
    public Error updateError(UUID deliveryId, Error error) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        if (delivery.getStatus().equals(DeliveryStatus.DELIVERED)) {
            // Error updates are not allowed for orders that are already delivered
            throw new DeliveryService.OrderAlreadyDeliveredException();
        }
        // We ensure error has the same UUID as the delivery
        error.setErrorId(deliveryId);
        delivery.setError(error);
        deliveryRepository.save(delivery);
        return delivery.getError();
    }

    /**
     * Exception to be used when an Error entity with a given ID is not found.
     */
    static public class ErrorNotFoundException extends ResponseStatusException {
        public ErrorNotFoundException() {
            super(HttpStatus.NOT_FOUND, "Error with specified id not found");
        }
    }

    /**
     * Persists an entity in the database
     * Used for testing purposes
     *
     * @param error Error to add to repository
     * @return the inserted error
     */
    public Error insert(Error error) {
        if (error == null || error.getErrorId() == null) {
            throw new IllegalArgumentException();
        }
        return errorRepository.save(error);
    }
}