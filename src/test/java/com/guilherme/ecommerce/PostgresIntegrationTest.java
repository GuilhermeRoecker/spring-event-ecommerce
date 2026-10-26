package com.guilherme.ecommerce;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Runs the same security and transaction scenarios against a disposable PostgreSQL database. */
@EnabledIfEnvironmentVariable(named = "TEST_POSTGRES_URL", matches = ".+")
class PostgresIntegrationTest extends EcommerceIntegrationTest {
  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", () -> System.getenv("TEST_POSTGRES_URL"));
    properties.add(
        "spring.datasource.username",
        () -> System.getenv().getOrDefault("TEST_POSTGRES_USER", "ecommerce"));
    properties.add("spring.datasource.password", () -> System.getenv("TEST_POSTGRES_PASSWORD"));
  }
}
