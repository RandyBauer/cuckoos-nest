package io.github.randybauer.cuckoosnest;

import org.springframework.boot.SpringApplication;

public class TestCuckoosNestApplication {

	public static void main(String[] args) {
		SpringApplication.from(CuckoosNestApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
