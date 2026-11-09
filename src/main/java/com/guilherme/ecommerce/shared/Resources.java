package com.guilherme.ecommerce.shared;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Resources {
  private Resources() {}

  public static <T> T require(Optional<T> resource) {
    return resource.orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso não encontrado"));
  }
}
