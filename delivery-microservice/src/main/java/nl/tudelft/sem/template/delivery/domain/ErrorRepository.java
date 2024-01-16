package nl.tudelft.sem.template.delivery.domain;

import java.util.UUID;
import nl.tudelft.sem.template.model.Error;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorRepository extends JpaRepository<Error, UUID> { }
