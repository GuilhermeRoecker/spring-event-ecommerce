package com.guilherme.ecommerce;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guilherme.ecommerce.commerce.CheckoutService;
import com.guilherme.ecommerce.commerce.CommerceController;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
    })
@AutoConfigureMockMvc
class EcommerceIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired PasswordEncoder encoder;
  @Autowired CheckoutService checkout;
  private String hash;

  @BeforeEach
  void seed() {
    for (String table :
        List.of(
            "coupon_redemption",
            "delivery",
            "payment",
            "order_item",
            "purchase_order",
            "cart_item",
            "address",
            "product",
            "coupon",
            "category",
            "account",
            "company")) jdbc.update("DELETE FROM " + table);
    hash = encoder.encode("customer-password");
    jdbc.update(
        "INSERT INTO company(id,name,legal_name) VALUES(1,'Loja A','Loja A Ltda'),(2,'Loja B','Loja"
            + " B Ltda')");
    jdbc.update("INSERT INTO category(id,name) VALUES(1,'Livros')");
    for (int i = 1; i <= 5; i++)
      jdbc.update(
          "INSERT INTO account(id,name,email,password_hash,role,company_id) VALUES(?,?,?,?,?,?)",
          i,
          "Pessoa " + i,
          "user" + i + "@test.com",
          hash,
          i == 3 ? "SELLER" : i == 4 ? "ADMIN" : i == 5 ? "OWNER" : "CUSTOMER",
          i == 3 ? Integer.valueOf(1) : i == 5 ? Integer.valueOf(2) : null);
    jdbc.update("ALTER TABLE account ALTER COLUMN id RESTART WITH 10");
    jdbc.update(
        "INSERT INTO product(id,company_id,category_id,name,description,price,stock)"
            + " VALUES(1,1,1,'Livro','Teste',10.50,5),(2,2,1,'Outro','Teste',20.00,2)");
    jdbc.update(
        "INSERT INTO address(id,account_id,street,number,city,state,postal_code) VALUES(1,1,'Rua"
            + " A','10','Orleans','SC','88870000'),(2,2,'Rua B','20','Orleans','SC','88870000')");
  }

  String body(Object o) throws Exception {
    return json.writeValueAsString(o);
  }

  String request() {
    return "{\"addressId\":1,\"method\":\"PIX\"}";
  }

  void cart(int account, int product, int qty) {
    jdbc.update(
        "INSERT INTO cart_item(account_id,product_id,quantity) VALUES(?,?,?)",
        account,
        product,
        qty);
  }

  @Test
  void anonymousCannotUseCheckout() throws Exception {
    mvc.perform(
            post("/api/me/checkout")
                .with(csrf())
                .header("Idempotency-Key", "request-0001")
                .contentType("application/json")
                .content(request()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void csrfRequiredEvenOnRegistration() throws Exception {
    mvc.perform(post("/api/auth/register").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void loginAndLogoutInvalidateSession() throws Exception {
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .param("email", "user1@test.com")
                    .param("password", "customer-password"))
            .andExpect(status().isNoContent())
            .andReturn();
    var session =
        (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
    mvc.perform(get("/api/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("user1@test.com"))
        .andExpect(jsonPath("$.password_hash").doesNotExist());
    mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  void registrationCannotChooseRoleOrSqlColumn() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"name\":\"Teste\",\"email\":\"new@test.com\",\"password\":\"long-password\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/api/me")
                .with(user("user1@test.com"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"name\":\"Teste\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registrationHashesPasswordAndAlwaysCreatesCustomer() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"name\":\"Teste\",\"email\":\"NEW@test.com\",\"password\":\"long-password\"}"))
        .andExpect(status().isCreated());
    var row = jdbc.queryForMap("SELECT role,password_hash FROM account WHERE email='new@test.com'");
    assertThat(row.get("role")).isEqualTo("CUSTOMER");
    assertThat(encoder.matches("long-password", (String) row.get("password_hash"))).isTrue();
  }

  @Test
  void privateListsAreScopedAndAddressCannotBeStolen() throws Exception {
    mvc.perform(get("/api/me/addresses").with(user("user1@test.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(1));
    mvc.perform(delete("/api/me/addresses/2").with(user("user1@test.com")).with(csrf()))
        .andExpect(status().isNotFound());
    cart(1, 1, 1);
    mvc.perform(
            post("/api/me/checkout")
                .with(user("user1@test.com"))
                .with(csrf())
                .header("Idempotency-Key", "request-0001")
                .contentType("application/json")
                .content("{\"addressId\":2,\"method\":\"PIX\"}"))
        .andExpect(status().isNotFound());
    assertThat(jdbc.queryForObject("SELECT stock FROM product WHERE id=1", Integer.class))
        .isEqualTo(5);
  }

  String productBody() {
    return "{\"companyId\":1,\"categoryId\":1,\"name\":\"Mudança\",\"description\":\"Teste\",\"price\":10.50,\"stock\":5,\"active\":true}";
  }

  @Test
  void customerCannotChangeProductAndSellerCannotCrossCompany() throws Exception {
    mvc.perform(
            put("/api/products/1")
                .with(user("user1@test.com"))
                .with(csrf())
                .contentType("application/json")
                .content(productBody()))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/products/2").with(user("user3@test.com")).with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/products/1")
                .with(user("user3@test.com"))
                .with(csrf())
                .contentType("application/json")
                .content(productBody()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/manage/order-items?companyId=2").with(user("user3@test.com")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminAuthorityComesFromDatabaseNotRequest() throws Exception {
    mvc.perform(get("/api/admin/accounts").with(user("user1@test.com").roles("ADMIN")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/accounts").with(user("user4@test.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].password_hash").doesNotExist());
  }

  @Test
  void catalogPaginationAndInjectionAreSafe() throws Exception {
    mvc.perform(get("/api/products?size=101")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/products?size=0")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/products").param("search", "' OR 1=1 --"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/products?size=1&page=1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(2));
  }

  @Test
  void quantityAndPriceValidation() throws Exception {
    mvc.perform(
            put("/api/me/cart/1")
                .with(user("user1@test.com"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"quantity\":-1}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/api/products/1")
                .with(user("user3@test.com"))
                .with(csrf())
                .contentType("application/json")
                .content(productBody().replace("10.50", "-1")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void checkoutIsAtomicIdempotentAndDoesNotInventPayment() throws Exception {
    cart(1, 1, 2);
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/me/checkout")
                  .with(user("user1@test.com"))
                  .with(csrf())
                  .header("Idempotency-Key", "request-0001")
                  .contentType("application/json")
                  .content(request()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.total").value(21))
          .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase_order", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT stock FROM product WHERE id=1", Integer.class))
        .isEqualTo(3);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_item", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT status FROM payment", String.class))
        .isEqualTo("PENDING");
    long id = jdbc.queryForObject("SELECT id FROM purchase_order", Long.class);
    mvc.perform(get("/api/me/orders/" + id).with(user("user2@test.com")))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/me/orders").with(user("user2@test.com")))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/me/orders/" + id).with(user("user1@test.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].unit_price").value(10.50));
  }

  @Test
  void unavailableStockRollsBackEverything() throws Exception {
    cart(1, 1, 2);
    cart(1, 2, 3);
    mvc.perform(
            post("/api/me/checkout")
                .with(user("user1@test.com"))
                .with(csrf())
                .header("Idempotency-Key", "request-0001")
                .contentType("application/json")
                .content(request()))
        .andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase_order", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT stock FROM product WHERE id=1", Integer.class))
        .isEqualTo(5);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_item", Integer.class)).isEqualTo(2);
  }

  @Test
  void couponIsAppliedOnceWithDecimalRounding() throws Exception {
    jdbc.update(
        "INSERT INTO coupon(id,code,percentage,expires_on,usage_limit) VALUES(1,'SAVE',15,DATE"
            + " '2099-12-31',1)");
    cart(1, 1, 1);
    mvc.perform(
            post("/api/me/checkout")
                .with(user("user1@test.com"))
                .with(csrf())
                .header("Idempotency-Key", "request-0001")
                .contentType("application/json")
                .content("{\"addressId\":1,\"method\":\"PIX\",\"coupon\":\"SAVE\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.total").value(8.92));
    assertThat(jdbc.queryForObject("SELECT used FROM coupon WHERE id=1", Integer.class))
        .isEqualTo(1);
    cart(2, 1, 1);
    mvc.perform(
            post("/api/me/checkout")
                .with(user("user2@test.com"))
                .with(csrf())
                .header("Idempotency-Key", "request-0002")
                .contentType("application/json")
                .content("{\"addressId\":2,\"method\":\"PIX\",\"coupon\":\"SAVE\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void paymentAndDeliveryTransitionsRequireAdmin() throws Exception {
    cart(1, 1, 1);
    mvc.perform(
            post("/api/me/checkout")
                .with(user("user1@test.com"))
                .with(csrf())
                .header("Idempotency-Key", "request-0001")
                .contentType("application/json")
                .content(request()))
        .andExpect(status().isCreated());
    long id = jdbc.queryForObject("SELECT id FROM purchase_order", Long.class);
    mvc.perform(
            post("/api/admin/orders/" + id + "/confirm-payment")
                .with(user("user1@test.com"))
                .with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/orders/" + id + "/confirm-payment")
                .with(user("user4@test.com"))
                .with(csrf()))
        .andExpect(status().isOk());
    for (String state : List.of("SHIPPED", "DELIVERED"))
      mvc.perform(
              put("/api/admin/orders/" + id + "/delivery")
                  .with(user("user4@test.com"))
                  .with(csrf())
                  .contentType("application/json")
                  .content(
                      "{\"status\":\""
                          + state
                          + "\",\"carrier\":\"Correios\",\"trackingCode\":\"TEST123\"}"))
          .andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT status FROM purchase_order", String.class))
        .isEqualTo("DELIVERED");
  }

  @Test
  void concurrentBuyersCannotOversell() throws Exception {
    jdbc.update("UPDATE product SET stock=1 WHERE id=1");
    cart(1, 1, 1);
    cart(2, 1, 1);
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      List<Future<Boolean>> futures = new ArrayList<>();
      for (int i = 1; i <= 2; i++) {
        final int n = i;
        futures.add(
            executor.submit(
                () -> {
                  SecurityContextHolder.getContext()
                      .setAuthentication(
                          new UsernamePasswordAuthenticationToken(
                              "user" + n + "@test.com", "", List.of()));
                  try {
                    start.await();
                    checkout.checkout(
                        new CommerceController.Checkout(
                            (long) n, CommerceController.Method.PIX, null),
                        "concurrent-" + n);
                    return true;
                  } catch (org.springframework.web.server.ResponseStatusException e) {
                    return false;
                  } finally {
                    SecurityContextHolder.clearContext();
                  }
                }));
      }
      start.countDown();
      int success = 0;
      for (var f : futures) if (f.get(10, TimeUnit.SECONDS)) success++;
      assertThat(success).isEqualTo(1);
      assertThat(jdbc.queryForObject("SELECT stock FROM product WHERE id=1", Integer.class))
          .isZero();
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase_order", Integer.class))
          .isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void passwordChangeRequiresCurrentPasswordAndHashesNewPassword() throws Exception {
    mvc.perform(
            put("/api/me/password")
                .with(user("user1@test.com"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"new-strong-password\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/api/me/password")
                .with(user("user1@test.com"))
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"currentPassword\":\"customer-password\",\"newPassword\":\"new-strong-password\"}"))
        .andExpect(status().isOk());
    assertThat(
            encoder.matches(
                "new-strong-password",
                jdbc.queryForObject("SELECT password_hash FROM account WHERE id=1", String.class)))
        .isTrue();
  }

  @Test
  void passwordLimitUsesUtf8Bytes() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(
                    body(
                        Map.of(
                            "name",
                            "Teste",
                            "email",
                            "unicode@test.com",
                            "password",
                            "é".repeat(40)))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void onlyHttpsImagesAreAccepted() throws Exception {
    mvc.perform(
            put("/api/products/1")
                .with(user("user3@test.com"))
                .with(csrf())
                .contentType("application/json")
                .content(
                    productBody()
                        .replace(
                            "\"active\":true",
                            "\"active\":true,\"imageUrl\":\"https://example.com/test.jpg\"")))
        .andExpect(status().isOk());
    mvc.perform(
            put("/api/products/1")
                .with(user("user3@test.com"))
                .with(csrf())
                .contentType("application/json")
                .content(
                    productBody()
                        .replace(
                            "\"active\":true",
                            "\"active\":true,\"imageUrl\":\"javascript:alert(1)\"")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void invalidIdempotencyKeyIsRejected() throws Exception {
    cart(1, 1, 1);
    mvc.perform(
            post("/api/me/checkout")
                .with(user("user1@test.com"))
                .with(csrf())
                .header("Idempotency-Key", "short")
                .contentType("application/json")
                .content(request()))
        .andExpect(status().isBadRequest());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase_order", Integer.class)).isZero();
  }

  @Test
  void storefrontAssetsArePublicWithoutOpeningPrivateRoutes() throws Exception {
    for (String path :
        List.of(
            "/",
            "/app.js",
            "/style.css",
            "/assets/js/ui.js",
            "/assets/js/management.js",
            "/assets/fonts/lexend.css",
            "/assets/images/bg_blobbubble_3.png")) {
      mvc.perform(get(path)).andExpect(status().isOk());
    }
    mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/admin/accounts")).andExpect(status().isUnauthorized());
  }
}
