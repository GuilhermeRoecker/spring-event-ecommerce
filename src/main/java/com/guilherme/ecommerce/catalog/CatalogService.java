package com.guilherme.ecommerce.catalog;

import com.guilherme.ecommerce.auth.CurrentAccount;
import com.guilherme.ecommerce.catalog.CatalogDtos.*;
import com.guilherme.ecommerce.shared.Database;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
  private final CatalogRepository repository;
  private final CurrentAccount current;

  public CatalogService(CatalogRepository repository, CurrentAccount current) {
    this.repository = repository;
    this.current = current;
  }

  public Object products(int page, int size, Long categoryId, Long companyId, String search) {
    return repository.findProducts(page, size, categoryId, companyId, search);
  }

  public Object product(long id) {
    return repository.findActiveProduct(id);
  }

  public Object managed(long companyId) {
    current.manage(companyId, false);
    return repository.findManagedProducts(companyId);
  }

  public void create(ProductInput p) {
    current.manage(p.companyId(), false);
    repository.requireCategory(p.categoryId());
    repository.insertProduct(
        p.companyId(),
        p.categoryId(),
        p.name(),
        p.description(),
        p.price(),
        p.stock(),
        p.active(),
        p.imageUrl());
  }

  @Transactional
  public void update(long id, ProductInput p) {
    var old = repository.lockProduct(id);
    current.manage(Database.id(old, "company_id"), false);
    if (Database.id(old, "company_id") != p.companyId())
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Não é permitido transferir produtos entre empresas");
    repository.requireCategoryForUpdate(p.categoryId());
    repository.updateProduct(
        p.categoryId(),
        p.name(),
        p.description(),
        p.price(),
        p.stock(),
        p.active(),
        p.imageUrl(),
        id);
  }

  @Transactional
  public void delete(long id) {
    var p = repository.lockProductForDeletion(id);
    current.manage(Database.id(p, "company_id"), false);
    repository.deactivateProduct(id);
  }

  public Object categories() {
    return repository.findCategories();
  }

  public void category(CategoryInput r) {
    current.admin();
    repository.insertCategory(r.name());
  }

  public void categoryUpdate(long id, CategoryInput r) {
    current.admin();
    repository.requireCategoryForUpdateById(id);
    repository.updateCategory(r.name(), id);
  }

  public void categoryDelete(long id) {
    current.admin();
    repository.requireCategoryForDeletion(id);
    repository.deleteCategory(id);
  }

  public Object companies() {
    return repository.findCompanies();
  }

  public void company(CompanyInput r) {
    current.admin();
    repository.insertCompany(r.name(), r.legalName());
  }

  public void companyUpdate(long id, CompanyInput r) {
    current.manage(id, true);
    repository.requireCompany(id);
    repository.updateCompany(r.name(), r.legalName(), id);
  }
}
