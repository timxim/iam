package it.infn.mw.iam.test;

import static com.jayway.restassured.RestAssured.given;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.ObjectMapperConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.mapper.factory.Jackson2ObjectMapperFactory;

import it.infn.mw.iam.api.scim.model.ScimMemberRef;
import it.infn.mw.iam.api.scim.model.ScimUser;

public class TestUtils {

  public static final int TOTAL_USERS_COUNT = 253;
  public static final String CLIENT_CRED_GRANT_CLIENT_ID = "client-cred";
  public static final String CLIENT_CRED_GRANT_CLIENT_SECRET = "secret";
  public static final String PASSWORD_GRANT_CLIENT_ID = "password-grant";
  public static final String PASSWORD_GRANT_CLIENT_SECRET = "secret";

  public static ObjectMapper createJacksonObjectMapper() {

    FilterProvider filters = new SimpleFilterProvider().setFailOnUnknownId(false);

    ObjectMapper mapper = new ObjectMapper();
    mapper.setFilterProvider(filters);
    return mapper;

  }

  public static Jackson2ObjectMapperFactory getJacksonObjectMapperFactory() {

    return new Jackson2ObjectMapperFactory() {

      @Override
      public ObjectMapper create(@SuppressWarnings("rawtypes") Class cls, String charset) {

        return createJacksonObjectMapper();
      }
    };
  }

  public static void initRestAssured() {

    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
        new ObjectMapperConfig().jackson2ObjectMapperFactory(getJacksonObjectMapperFactory()));

  }

  public static ScimMemberRef getMemberRef(ScimUser user) {

    return ScimMemberRef.builder()
      .display(user.getDisplayName())
      .ref(user.getMeta().getLocation())
      .value(user.getId())
      .build();
  }

  public static List<ScimMemberRef> buildScimMemberRefList(List<ScimUser> users) {

    List<ScimMemberRef> membersRefs = new ArrayList<ScimMemberRef>();
    for (ScimUser u : users) {
      membersRefs.add(getMemberRef(u));
    }
    return membersRefs;
  }

  public static String getAccessToken(String clientId, String clientSecret, String scope) {
    return clientCredentialsTokenGetter(clientId, clientSecret).scope(scope).getAccessToken();
  }

  public static AccessTokenGetter clientCredentialsTokenGetter(String clientId,
      String clientSecret) {
    return new AccessTokenGetter(clientId, clientSecret).grantType("client_credentials");
  }

  public static AccessTokenGetter clientCredentialsTokenGetter() {
    return new AccessTokenGetter(CLIENT_CRED_GRANT_CLIENT_ID, CLIENT_CRED_GRANT_CLIENT_SECRET)
      .grantType("client_credentials");
  }


  public static AccessTokenGetter passwordTokenGetter() {
    return new AccessTokenGetter(PASSWORD_GRANT_CLIENT_ID, PASSWORD_GRANT_CLIENT_SECRET)
      .grantType("password");
  }

  public static AccessTokenGetter passwordTokenGetter(String clientId, String clientSecret) {
    return new AccessTokenGetter(clientId, clientSecret).grantType("password");
  }

  public static class AccessTokenGetter {
    private String clientId;
    private String clientSecret;
    private String scope;
    private String grantType;
    private String username;
    private String password;
    private int port = 8080;

    public AccessTokenGetter(String clientId, String clientSecret) {
      this.clientId = clientId;
      this.clientSecret = clientSecret;
    }

    public AccessTokenGetter scope(String scope) {
      this.scope = scope;
      return this;
    }

    public AccessTokenGetter grantType(String grantType) {
      this.grantType = grantType;
      return this;
    }

    public AccessTokenGetter username(String username) {
      this.username = username;
      return this;
    }

    public AccessTokenGetter password(String password) {
      this.password = password;
      return this;
    }


    public String getAccessToken() {

      switch (grantType) {
        case "client_credentials":
          return given().port(port)
            .param("grant_type", grantType)
            .param("client_id", clientId)
            .param("client_secret", clientSecret)
            .param("scope", scope)
            .when()
            .post("/token")
            .then()
            .log()
            .all(true)
            .statusCode(HttpStatus.OK.value())
            .extract()
            .path("access_token");

        case "password":
          return given().port(port)
            .param("grant_type", grantType)
            .param("client_id", clientId)
            .param("client_secret", clientSecret)
            .param("scope", scope)
            .param("username", username)
            .param("password", password)
            .when()
            .post("/token")
            .then()
            .log()
            .all(true)
            .statusCode(HttpStatus.OK.value())
            .extract()
            .path("access_token");

        default:
          throw new IllegalArgumentException("Unsupported grant type: " + grantType);
      }
    }
  }
}
