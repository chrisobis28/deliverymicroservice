package nl.tudelft.sem.template.delivery.domain;

import java.util.List;
import java.util.Optional;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {

    @Query("SELECT e FROM Restaurant e WHERE :courierId MEMBER OF e.couriers")
    Optional<Restaurant> findRestaurantByCouriersContains(@Param("courierId") String courierId);

    @Query("SELECT e FROM Restaurant e WHERE e.couriers IS NOT EMPTY")
    List<Restaurant> findRestaurantsByCouriersIsNotEmpty();
}