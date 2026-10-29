package com.guilherme.ecommerce.catalog;

import com.guilherme.ecommerce.catalog.CatalogDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
  private final CatalogService service;

  public CatalogController(CatalogService service) {
    this.service = service;
  }

  @GetMapping("/products")
  public Object products(
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long companyId,
      @RequestParam(defaultValue = "") @Size(max = 100) String search) {
    return service.products(page, size, categoryId, companyId, search);
  }

  @GetMapping("/products/{id}")
  public Object product(@PathVariable long id) {
    return service.product(id);
  }

  @GetMapping("/manage/products")
  public Object managed(@RequestParam long companyId) {
    return service.managed(companyId);
  }

  @PostMapping("/products")
  @ResponseStatus(HttpStatus.CREATED)
  public void create(@Valid @RequestBody ProductInput p) {
    service.create(p);
  }

  @PutMapping("/products/{id}")
  public void update(@PathVariable long id, @Valid @RequestBody ProductInput p) {
    service.update(id, p);
  }

  @DeleteMapping("/products/{id}")
  public void delete(@PathVariable long id) {
    service.delete(id);
  }

  @GetMapping("/categories")
  public Object categories() {
    return service.categories();
  }

  @PostMapping("/categories")
  @ResponseStatus(HttpStatus.CREATED)
  public void category(@Valid @RequestBody CategoryInput r) {
    service.category(r);
  }

  @PutMapping("/categories/{id}")
  public void categoryUpdate(@PathVariable long id, @Valid @RequestBody CategoryInput r) {
    service.categoryUpdate(id, r);
  }

  @DeleteMapping("/categories/{id}")
  public void categoryDelete(@PathVariable long id) {
    service.categoryDelete(id);
  }

  @GetMapping("/companies")
  public Object companies() {
    return service.companies();
  }

  @PostMapping("/companies")
  @ResponseStatus(HttpStatus.CREATED)
  public void company(@Valid @RequestBody CompanyInput r) {
    service.company(r);
  }

  @PutMapping("/companies/{id}")
  public void companyUpdate(@PathVariable long id, @Valid @RequestBody CompanyInput r) {
    service.companyUpdate(id, r);
  }
}
