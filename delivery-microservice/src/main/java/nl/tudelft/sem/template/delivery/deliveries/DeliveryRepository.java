package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> { }
