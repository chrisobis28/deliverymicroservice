package nl.tudelft.sem.template.delivery.domain;

import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {
}
