package io.github.randybauer.cuckoosnest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CuckoosNestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuckoosNestApplication.class, args);
    }
}
