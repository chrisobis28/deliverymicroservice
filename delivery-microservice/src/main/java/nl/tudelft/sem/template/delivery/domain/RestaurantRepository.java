package nl.tudelft.sem.template.delivery.domain;

import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {

  @Query("SELECT e FROM Restaurant e WHERE :courierId MEMBER OF e.couriers")
  Optional<Restaurant> findRestaurantByCouriersContains(@Param("courierId") String courierId);
}