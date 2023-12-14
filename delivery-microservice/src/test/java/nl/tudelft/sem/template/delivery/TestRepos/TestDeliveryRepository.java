package nl.tudelft.sem.template.delivery.TestRepos;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestDeliveryRepository implements DeliveryRepository {

    private final List<Delivery> deliveryList = new ArrayList<>();

    @Override
    public List<Delivery> findAll() {
        return deliveryList;
    }

    @Override
    public List<Delivery> findAll(Sort sort) {
        // Implement sorting if needed
        return deliveryList;
    }

    @Override
    public Page<Delivery> findAll(Pageable pageable) {
        // Implement paging if needed
        return null;
    }

    @Override
    public List<Delivery> findAllById(Iterable<UUID> uuids) {
        // Implement finding by IDs if needed
        return null;
    }

    @Override
    public long count() {
        return deliveryList.size();
    }

    @Override
    public void deleteById(UUID uuid) {
        deliveryList.removeIf(delivery -> delivery.getDeliveryID().equals(uuid));
    }

    @Override
    public void delete(Delivery entity) {
        deliveryList.remove(entity);
    }

    @Override
    public void deleteAll(Iterable<? extends Delivery> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public <S extends Delivery> S save(S entity) {
        deliveryList.add(entity);
        return entity;
    }

    @Override
    public <S extends Delivery> List<S> saveAll(Iterable<S> entities) {
        List<S> savedEntities = new ArrayList<>();
        entities.forEach(entity -> {
            S savedEntity = save(entity);
            savedEntities.add(savedEntity);
        });
        return savedEntities;
    }

    @Override
    public Optional<Delivery> findById(UUID uuid) {
        return deliveryList.stream()
                .filter(delivery -> delivery.getDeliveryID().equals(uuid))
                .findFirst();
    }

    @Override
    public boolean existsById(UUID uuid) {
        return deliveryList.stream().anyMatch(delivery -> delivery.getDeliveryID().equals(uuid));
    }



    @Override
    public void flush() {

    }

    @Override
    public <S extends Delivery> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public void deleteInBatch(Iterable<Delivery> entities) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public Delivery getOne(UUID uuid) {
        return null;
    }

    @Override
    public <S extends Delivery> Optional<S> findOne(Example<S> example) {
        return (Optional<S>) deliveryList.stream()
                .filter(example.getProbe()::equals)
                .findFirst();
    }

    @Override
    public <S extends Delivery> List<S> findAll(Example<S> example) {
        List<S> result = new ArrayList<>();
        for (Delivery delivery : deliveryList) {
            if (example.getProbe().equals(delivery)) {
                result.add((S) delivery);
            }
        }
        return result;
    }

    @Override
    public <S extends Delivery> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends Delivery> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends Delivery> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Delivery> boolean exists(Example<S> example) {
        return false;
    }
}
