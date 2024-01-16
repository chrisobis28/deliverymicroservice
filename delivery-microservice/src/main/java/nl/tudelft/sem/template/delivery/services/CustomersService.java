package nl.tudelft.sem.template.delivery.services;

import java.util.List;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.stereotype.Service;

@Service
public class CustomersService {

    private final transient DeliveryRepository deliveryRepository;

    public CustomersService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public List<Delivery> getDeliveriesForCustomer(String customerId) {
        return deliveryRepository.findAllByCustomerID(customerId);
    }
}
