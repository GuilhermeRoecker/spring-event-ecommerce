package com.guilherme.ecommerce.catalog.service;

import static com.guilherme.ecommerce.shared.Resources.require;

import com.guilherme.ecommerce.account.domain.Company;
import com.guilherme.ecommerce.account.repository.CompanyRepository;
import com.guilherme.ecommerce.account.security.CurrentAccount;
import com.guilherme.ecommerce.catalog.domain.Category;
import com.guilherme.ecommerce.catalog.domain.Product;
import com.guilherme.ecommerce.catalog.dto.request.CategoryInput;
import com.guilherme.ecommerce.catalog.dto.request.CompanyInput;
import com.guilherme.ecommerce.catalog.dto.request.ProductInput;
import com.guilherme.ecommerce.catalog.dto.response.CategoryResponse;
import com.guilherme.ecommerce.catalog.dto.response.CompanyResponse;
import com.guilherme.ecommerce.catalog.dto.response.ManagedProductResponse;
import com.guilherme.ecommerce.catalog.dto.response.ProductResponse;
import com.guilherme.ecommerce.catalog.repository.CategoryRepository;
import com.guilherme.ecommerce.catalog.repository.ProductRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(readOnly = true)
public class CatalogService {
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final CompanyRepository companies;
  private final CurrentAccount current;

  public CatalogService(
      ProductRepository products,
      CategoryRepository categories,
      CompanyRepository companies,
      CurrentAccount current) {
    this.products = products;
    this.categories = categories;
    this.companies = companies;
    this.current = current;
  }

  public List<ProductResponse> products(
      int page, int size, Long categoryId, Long companyId, String search) {
    return products.catalog(categoryId, companyId, search, PageRequest.of(page, size)).stream()
        .map(this::view)
        .toList();
  }

  public ProductResponse product(long id) {
    return view(require(products.findByIdAndActiveTrue(id)));
  }

  public List<ManagedProductResponse> managed(long companyId) {
    current.manage(companyId, false);
    return products.findByCompanyIdOrderByIdAsc(companyId, PageRequest.of(0, 100)).stream()
        .map(
            p ->
                new ManagedProductResponse(
                    p.getId(),
                    p.getCompany().getId(),
                    p.getCategory().getId(),
                    p.getName(),
                    p.getDescription(),
                    p.getPrice(),
                    p.getStock(),
                    p.getActive(),
                    p.getImageUrl()))
        .toList();
  }

  @Transactional
  public void create(@Valid ProductInput p) {
    current.manage(p.companyId(), false);
    products.save(
        new Product(
            require(companies.findById(p.companyId())),
            require(categories.findById(p.categoryId())),
            p.name(),
            p.description(),
            p.price(),
            p.stock(),
            p.active(),
            p.imageUrl()));
  }

  @Transactional
  public void update(long id, @Valid ProductInput p) {
    var old = require(products.lockById(id));
    current.manage(old.getCompany().getId(), false);
    if (!old.getCompany().getId().equals(p.companyId()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Não é permitido transferir produtos entre empresas");
    old.revise(
        require(categories.findById(p.categoryId())),
        p.name(),
        p.description(),
        p.price(),
        p.stock(),
        p.active(),
        p.imageUrl());
  }

  @Transactional
  public void delete(long id) {
    var p = require(products.lockById(id));
    current.manage(p.getCompany().getId(), false);
    p.deactivate();
  }

  public List<CategoryResponse> categories() {
    return categories.findAllByOrderByNameAsc(PageRequest.of(0, 500)).stream()
        .map(c -> new CategoryResponse(c.getId(), c.getName()))
        .toList();
  }

  @Transactional
  public void category(@Valid CategoryInput r) {
    current.admin();
    categories.save(new Category(r.name()));
  }

  @Transactional
  public void categoryUpdate(long id, @Valid CategoryInput r) {
    current.admin();
    require(categories.findById(id)).rename(r.name());
  }

  @Transactional
  public void categoryDelete(long id) {
    current.admin();
    categories.delete(require(categories.findById(id)));
  }

  public List<CompanyResponse> companies() {
    return companies.findAllByOrderByIdAsc(PageRequest.of(0, 500)).stream()
        .map(c -> new CompanyResponse(c.getId(), c.getName(), c.getLegalName()))
        .toList();
  }

  @Transactional
  public void company(@Valid CompanyInput r) {
    current.admin();
    companies.save(new Company(r.name(), r.legalName()));
  }

  @Transactional
  public void companyUpdate(long id, @Valid CompanyInput r) {
    current.manage(id, true);
    require(companies.findById(id)).rename(r.name(), r.legalName());
  }

  private ProductResponse view(Product p) {
    return new ProductResponse(
        p.getId(),
        p.getCompany().getId(),
        p.getCategory().getId(),
        p.getName(),
        p.getDescription(),
        p.getPrice(),
        p.getStock(),
        p.getImageUrl());
  }
}
