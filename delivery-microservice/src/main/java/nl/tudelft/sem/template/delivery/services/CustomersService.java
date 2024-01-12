package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomersService {

    private final DeliveryRepository deliveryRepository;

    public CustomersService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public List<Delivery> getDeliveriesForCustomer(String customerID) {
        return deliveryRepository.findAllByCustomerID(customerID);
    }
}
