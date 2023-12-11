package nl.tudelft.sem.template.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Example microservice application.
 */
@SpringBootApplication
@EntityScan(basePackages = "nl.tudelft.sem.template.model")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
