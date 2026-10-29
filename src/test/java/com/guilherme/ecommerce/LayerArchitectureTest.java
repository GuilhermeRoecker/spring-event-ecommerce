package com.guilherme.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import com.guilherme.ecommerce.auth.*;
import com.guilherme.ecommerce.catalog.*;
import com.guilherme.ecommerce.commerce.*;
import com.guilherme.ecommerce.shared.Database;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

class LayerArchitectureTest {
  private static final List<Class<?>> CONTROLLERS =
      List.of(
          AccountController.class,
          CatalogController.class,
          CommerceController.class,
          OperationsController.class);
  private static final List<Class<?>> SERVICES =
      List.of(
          AccountService.class,
          CatalogService.class,
          CommerceService.class,
          OperationsService.class,
          CheckoutService.class);

  @Test
  void controllersDelegateToServices() {
    for (var controller : CONTROLLERS) {
      for (var field : controller.getDeclaredFields()) {
        assertThat(field.getType().isAnnotationPresent(Service.class))
            .as("%s.%s must delegate to a service", controller.getSimpleName(), field.getName())
            .isTrue();
      }
      for (var method : controller.getDeclaredMethods()) {
        assertThat(method.isAnnotationPresent(Transactional.class)).isFalse();
      }
    }
  }

  @Test
  void servicesDoNotDependOnControllersOrDatabaseAccess() {
    for (var service : SERVICES) {
      for (var field : service.getDeclaredFields()) {
        assertThat(field.getType()).isNotEqualTo(Database.class);
        assertThat(field.getType().isAnnotationPresent(RestController.class)).isFalse();
      }
      for (var method : service.getDeclaredMethods()) {
        assertThat(
                Arrays.stream(method.getParameterTypes())
                    .map(Class::getEnclosingClass)
                    .filter(type -> type != null && CONTROLLERS.contains(type)))
            .as("%s must use DTOs independent of controllers", method.getName())
            .isEmpty();
      }
    }
  }
}
