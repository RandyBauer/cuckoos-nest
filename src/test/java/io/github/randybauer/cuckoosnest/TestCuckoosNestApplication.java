package io.github.randybauer.cuckoosnest;

import org.springframework.boot.SpringApplication;

/** Starts the app locally with throwaway Postgres and Redis containers ({@code ./gradlew bootTestRun}). */
public final class TestCuckoosNestApplication {

    private TestCuckoosNestApplication() {}

    public static void main(String[] args) {
        SpringApplication.from(CuckoosNestApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
