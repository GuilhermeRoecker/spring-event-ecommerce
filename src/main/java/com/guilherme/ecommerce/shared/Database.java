package com.guilherme.ecommerce.shared;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class Database {
  private final JdbcClient jdbc;

  public Database(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> list(String sql, Object... params) {
    return jdbc.sql(sql).params(Arrays.asList(params)).query().listOfRows();
  }

  public Map<String, Object> one(String sql, Object... params) {
    var rows = list(sql, params);
    if (rows.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso não encontrado");
    return rows.getFirst();
  }

  public int update(String sql, Object... params) {
    return jdbc.sql(sql).params(Arrays.asList(params)).update();
  }

  public long id(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }
}
