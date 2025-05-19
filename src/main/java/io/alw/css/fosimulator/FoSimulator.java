package io.alw.css.fosimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "io.alw.css.fosimulator")
@ConfigurationPropertiesScan("io.alw.css.fosimulator.model.properties")
public class FoSimulator {
    public static void main(String[] args) {
        SpringApplication.run(FoSimulator.class, args);
    }
}
