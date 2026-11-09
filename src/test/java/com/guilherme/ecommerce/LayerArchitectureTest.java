package com.guilherme.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import com.guilherme.ecommerce.account.controller.AccountController;
import com.guilherme.ecommerce.account.service.AccountService;
import com.guilherme.ecommerce.catalog.controller.CatalogController;
import com.guilherme.ecommerce.catalog.service.CatalogService;
import com.guilherme.ecommerce.checkout.controller.CheckoutController;
import com.guilherme.ecommerce.checkout.service.CheckoutService;
import com.guilherme.ecommerce.commerce.controller.AddressController;
import com.guilherme.ecommerce.commerce.controller.CartController;
import com.guilherme.ecommerce.commerce.service.AddressService;
import com.guilherme.ecommerce.commerce.service.CartService;
import com.guilherme.ecommerce.coupon.controller.CouponController;
import com.guilherme.ecommerce.coupon.service.CouponService;
import com.guilherme.ecommerce.order.controller.OrderController;
import com.guilherme.ecommerce.order.controller.OrderOperationsController;
import com.guilherme.ecommerce.order.service.OrderOperationsService;
import com.guilherme.ecommerce.order.service.OrderService;
import jakarta.persistence.EntityManager;
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
          AddressController.class,
          CartController.class,
          CheckoutController.class,
          OrderController.class,
          CouponController.class,
          OrderOperationsController.class);
  private static final List<Class<?>> SERVICES =
      List.of(
          AccountService.class,
          CatalogService.class,
          AddressService.class,
          CartService.class,
          OrderService.class,
          OrderOperationsService.class,
          CouponService.class,
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
        assertThat(field.getType()).isNotEqualTo(EntityManager.class);
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

  @Test
  void allControllersHaveTypedContractsAndOnlyInjectServices() throws Exception {
    for (Class<?> controller :
        annotated(org.springframework.web.bind.annotation.RestController.class)) {
      for (var field : controller.getDeclaredFields())
        assertThat(field.getType().isAnnotationPresent(Service.class)).isTrue();
      for (var method : controller.getDeclaredMethods()) {
        assertThat(method.getReturnType()).isNotEqualTo(Object.class);
        assertTransportType(method.getGenericReturnType());
      }
    }
  }

  private void assertTransportType(java.lang.reflect.Type type) {
    if (type instanceof Class<?> c) {
      assertThat(c.isAnnotationPresent(jakarta.persistence.Entity.class)).isFalse();
      assertThat(java.util.Map.class.isAssignableFrom(c)).isFalse();
    } else if (type instanceof java.lang.reflect.ParameterizedType p) {
      assertTransportType(p.getRawType());
      for (var argument : p.getActualTypeArguments()) assertTransportType(argument);
    }
  }

  private List<Class<?>> annotated(Class<? extends java.lang.annotation.Annotation> annotation)
      throws Exception {
    var scanner =
        new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(
            false);
    scanner.addIncludeFilter(
        new org.springframework.core.type.filter.AnnotationTypeFilter(annotation));
    var classes = new java.util.ArrayList<Class<?>>();
    for (var candidate : scanner.findCandidateComponents("com.guilherme.ecommerce"))
      classes.add(Class.forName(candidate.getBeanClassName()));
    return classes;
  }

  @Test
  void productionCodeDoesNotUseJdbcAndFeatureDependenciesAreAcyclic() throws Exception {
    var root = java.nio.file.Path.of("src/main/java/com/guilherme/ecommerce");
    var graph = new java.util.HashMap<String, java.util.Set<String>>();
    var reference = java.util.regex.Pattern.compile("com\\.guilherme\\.ecommerce\\.(\\w+)\\.");
    try (var files = java.nio.file.Files.walk(root)) {
      for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        var source = java.nio.file.Files.readString(file);
        assertThat(source).doesNotContain("JdbcClient", "JdbcTemplate", "java.sql.");
        var relative = root.relativize(file);
        if (relative.getNameCount() < 2) continue;
        var feature = relative.getName(0).toString();
        var dependencies = graph.computeIfAbsent(feature, k -> new java.util.HashSet<>());
        var matcher = reference.matcher(source);
        while (matcher.find())
          if (!feature.equals(matcher.group(1))) dependencies.add(matcher.group(1));
        if (relative.toString().contains("/domain/"))
          assertThat(source)
              .doesNotContain("org.springframework.", ".controller.", ".service.", ".repository.");
      }
    }
    for (var feature : graph.keySet())
      visit(feature, graph, new java.util.HashSet<>(), new java.util.HashSet<>());
  }

  private void visit(
      String feature,
      java.util.Map<String, java.util.Set<String>> graph,
      java.util.Set<String> path,
      java.util.Set<String> checked) {
    if (checked.contains(feature)) return;
    assertThat(path.add(feature))
        .as("circular feature dependency: %s -> %s", path, feature)
        .isTrue();
    for (var next : graph.getOrDefault(feature, java.util.Set.of()))
      visit(next, graph, path, checked);
    path.remove(feature);
    checked.add(feature);
  }
}
