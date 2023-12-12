package nl.tudelft.sem.template.delivery.domain.TestRepos;

import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestRestaurantRepository implements RestaurantRepository {

    private final List<Restaurant> restaurantList = new ArrayList<>();

    @Override
    public List<Restaurant> findAll() {
        return restaurantList;
    }

    @Override
    public List<Restaurant> findAll(Sort sort) {
        // Implement sorting if needed
        return restaurantList;
    }

    @Override
    public Page<Restaurant> findAll(Pageable pageable) {
        // Implement paging if needed
        return null;
    }

    @Override
    public List<Restaurant> findAllById(Iterable<String> restaurantIds) {
        List<Restaurant> result = new ArrayList<>();
        for (String restaurantId : restaurantIds) {
            findById(restaurantId).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public long count() {
        return restaurantList.size();
    }

    @Override
    public void deleteById(String restaurantId) {
        restaurantList.removeIf(restaurant -> restaurant.getRestaurantID().equals(restaurantId));
    }

    @Override
    public void delete(Restaurant entity) {
        restaurantList.remove(entity);
    }

    @Override
    public void deleteAll(Iterable<? extends Restaurant> entities) {
        restaurantList.removeAll((List<Restaurant>) entities);
    }

    @Override
    public void deleteAll() {
        restaurantList.clear();
    }

    @Override
    public <S extends Restaurant> S save(S entity) {
        restaurantList.add(entity);
        return entity;
    }

    @Override
    public <S extends Restaurant> List<S> saveAll(Iterable<S> entities) {
        List<S> savedEntities = new ArrayList<>();
        entities.forEach(entity -> {
            S savedEntity = save(entity);
            savedEntities.add(savedEntity);
        });
        return savedEntities;
    }

    @Override
    public Optional<Restaurant> findById(String restaurantId) {
        return restaurantList.stream()
                .filter(restaurant -> restaurant.getRestaurantID().equals(restaurantId))
                .findFirst();
    }

    @Override
    public boolean existsById(String restaurantId) {
        return restaurantList.stream().anyMatch(restaurant -> restaurant.getRestaurantID().equals(restaurantId));
    }

    @Override
    public void flush() {
        // Implement if necessary (for flushing changes to the underlying data store)
    }

    @Override
    public <S extends Restaurant> S saveAndFlush(S entity) {
        // Implement if necessary (for saving and immediately flushing changes)
        return null;
    }

    @Override
    public void deleteInBatch(Iterable<Restaurant> entities) {
        // Implement if necessary (for batch deletion)
    }

    @Override
    public void deleteAllInBatch() {
        // Implement if necessary (for batch deletion)
    }

    @Override
    public Restaurant getOne(String restaurantId) {
        // Implement if necessary (for retrieving a proxy reference to an entity)
        return null;
    }

    @Override
    public <S extends Restaurant> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends Restaurant> List<S> findAll(Example<S> example) {
        List<S> result = new ArrayList<>();
        for (Restaurant restaurant : restaurantList) {
            if (example.getProbe().equals(restaurant)) {
                result.add((S) restaurant);
            }
        }
        return result;
    }

    @Override
    public <S extends Restaurant> List<S> findAll(Example<S> example, Sort sort) {
        // Implement if necessary (for sorting based on example)
        return null;
    }

    @Override
    public <S extends Restaurant> Page<S> findAll(Example<S> example, Pageable pageable) {
        // Implement if necessary (for pagination based on example)
        return null;
    }

    @Override
    public <S extends Restaurant> long count(Example<S> example) {
        return findAll(example).size();
    }

    @Override
    public <S extends Restaurant> boolean exists(Example<S> example) {
        return !findAll(example).isEmpty();
    }
}
