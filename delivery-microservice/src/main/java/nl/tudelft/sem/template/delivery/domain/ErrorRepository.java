package nl.tudelft.sem.template.delivery.domain;

import nl.tudelft.sem.template.model.Error;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ErrorRepository extends JpaRepository<Error, UUID> { }
