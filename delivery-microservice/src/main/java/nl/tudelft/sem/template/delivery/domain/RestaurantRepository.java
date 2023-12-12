package nl.tudelft.sem.template.delivery.domain;

import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {
}
