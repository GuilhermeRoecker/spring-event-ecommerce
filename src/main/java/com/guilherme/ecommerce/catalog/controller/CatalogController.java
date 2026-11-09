package com.guilherme.ecommerce.catalog.controller;

import com.guilherme.ecommerce.catalog.dto.request.CategoryInput;
import com.guilherme.ecommerce.catalog.dto.request.CompanyInput;
import com.guilherme.ecommerce.catalog.dto.request.ProductInput;
import com.guilherme.ecommerce.catalog.dto.response.CategoryResponse;
import com.guilherme.ecommerce.catalog.dto.response.CompanyResponse;
import com.guilherme.ecommerce.catalog.dto.response.ManagedProductResponse;
import com.guilherme.ecommerce.catalog.dto.response.ProductResponse;
import com.guilherme.ecommerce.catalog.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {
  private final CatalogService service;

  public CatalogController(CatalogService service) {
    this.service = service;
  }

  @GetMapping("/products")
  public List<ProductResponse> products(
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long companyId,
      @RequestParam(defaultValue = "") @Size(max = 100) String search) {
    return service.products(page, size, categoryId, companyId, search);
  }

  @GetMapping("/products/{id}")
  public ProductResponse product(@PathVariable long id) {
    return service.product(id);
  }

  @GetMapping("/manage/products")
  public List<ManagedProductResponse> managed(@RequestParam long companyId) {
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
  public List<CategoryResponse> categories() {
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
  public List<CompanyResponse> companies() {
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
