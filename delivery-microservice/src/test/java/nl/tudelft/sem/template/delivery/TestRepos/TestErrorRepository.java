package nl.tudelft.sem.template.delivery.TestRepos;

import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
import nl.tudelft.sem.template.model.Error;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestErrorRepository implements ErrorRepository {

    private final List<Error> errorList = new ArrayList<>();

    @Override
    public List<Error> findAll() {
        return errorList;
    }

    @Override
    public List<Error> findAll(Sort sort) {
        // Implement sorting if needed
        return errorList;
    }

    @Override
    public Page<Error> findAll(Pageable pageable) {
        // Implement paging if needed
        return null;
    }

    @Override
    public List<Error> findAllById(Iterable<UUID> uuids) {
        // Implement finding by IDs if needed
        return null;
    }

    @Override
    public long count() {
        return errorList.size();
    }

    @Override
    public void deleteById(UUID uuid) {
        errorList.removeIf(error -> error.getErrorId().equals(uuid));
    }

    @Override
    public void delete(Error entity) {
        errorList.remove(entity);
    }

    @Override
    public void deleteAll(Iterable<? extends Error> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public <S extends Error> S save(S entity) {
        errorList.add(entity);
        return entity;
    }

    @Override
    public <S extends Error> List<S> saveAll(Iterable<S> entities) {
        List<S> savedEntities = new ArrayList<>();
        entities.forEach(entity -> {
            S savedEntity = save(entity);
            savedEntities.add(savedEntity);
        });
        return savedEntities;
    }

    @Override
    public Optional<Error> findById(UUID uuid) {
        return errorList.stream()
                .filter(error -> error.getErrorId().equals(uuid))
                .findFirst();
    }

    @Override
    public boolean existsById(UUID uuid) {
        return errorList.stream().anyMatch(error -> error.getErrorId().equals(uuid));
    }


    @Override
    public void flush() {

    }

    @Override
    public <S extends Error> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public void deleteInBatch(Iterable<Error> entities) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public Error getOne(UUID uuid) {
        return null;
    }

    @Override
    public <S extends Error> Optional<S> findOne(Example<S> example) {
        return (Optional<S>) errorList.stream()
                .filter(example.getProbe()::equals)
                .findFirst();
    }

    @Override
    public <S extends Error> List<S> findAll(Example<S> example) {
        List<S> result = new ArrayList<>();
        for (Error error : errorList) {
            if (example.getProbe().equals(error)) {
                result.add((S) error);
            }
        }
        return result;
    }

    @Override
    public <S extends Error> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends Error> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends Error> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Error> boolean exists(Example<S> example) {
        return false;
    }
}
