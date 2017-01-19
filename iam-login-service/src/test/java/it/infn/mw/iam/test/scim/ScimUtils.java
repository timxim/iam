package it.infn.mw.iam.test.scim;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import it.infn.mw.iam.api.scim.model.ScimUser;

public class ScimUtils {

  public static String SCIM_CONTENT_TYPE = "application/scim+json";

  public static final String SCIM_CLIENT_ID = "scim-client-rw";
  public static final String SCIM_CLIENT_SECRET = "secret";
  public static final String SCIM_READ_SCOPE = "scim:read";
  public static final String SCIM_WRITE_SCOPE = "scim:write";

  public static final String SCIM_ENDPOINT_BASEURL = "/scim";

  public static String getUsersLocation() {

    return SCIM_ENDPOINT_BASEURL + "/Users";
  }

  public static String getGroupsLocation() {

    return SCIM_ENDPOINT_BASEURL + "/Groups";
  }

  public static String getMeLocation() {

    return SCIM_ENDPOINT_BASEURL + "/Me";
  }

  public static String getUserLocation(String uuid) {

    return getUsersLocation() + "/" + uuid;
  }

  public static String getGroupLocation(String uuid) {

    return getGroupsLocation() + "/" + uuid;
  }

  public static ScimUser buildUser(String username, String email, String givenName,
      String familyName) {

    return ScimUser.builder(username).buildEmail(email).buildName(givenName, familyName).build();
  }

  public static ScimUser buildUserWithUUID(String uuid, String username, String email,
      String givenName, String familyName) {

    return ScimUser.builder(username)
      .id(uuid)
      .buildEmail(email)
      .buildName(givenName, familyName)
      .build();
  }

  public static ScimUser buildUserWithPassword(String username, String password, String email,
      String givenName, String familyName) {

    return ScimUser.builder(username)
      .password(password)
      .buildEmail(email)
      .buildName(givenName, familyName)
      .build();
  }

  public static class ParamsBuilder {

    private MultiValueMap<String, String> params;

    public static ParamsBuilder builder() {
      return new ParamsBuilder();
    }

    private ParamsBuilder() {
      params = new LinkedMultiValueMap<String, String>();
    }

    public ParamsBuilder count(int count) {
      params.add("count", String.valueOf(count));
      return this;
    }

    public ParamsBuilder startIndex(int startIndex) {
      params.add("startIndex", String.valueOf(startIndex));
      return this;
    }

    public ParamsBuilder attributes(String attributes) {
      params.add("attributes", attributes);
      return this;
    }

    public MultiValueMap<String, String> build() {
      return params;
    }
  }
}
