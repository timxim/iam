package it.infn.mw.iam.api.scim.model;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ScimOidcId {

  @NotEmpty
  private final String issuer;
  @NotEmpty
  private final String subject;

  @JsonCreator
  private ScimOidcId(@JsonProperty("issuer") String issuer,
      @JsonProperty("subject") String subject) {

    this.issuer = issuer;
    this.subject = subject;
  }

  private ScimOidcId(Builder b) {
    this.issuer = b.issuer;
    this.subject = b.subject;
  }

  public String getIssuer() {

    return issuer;
  }

  public String getSubject() {

    return subject;
  }

  public static Builder builder() {

    return new Builder();
  }

  public static class Builder {

    private String issuer;
    private String subject;

    public Builder issuer(String issuer) {

      this.issuer = issuer;
      return this;
    }

    public Builder subject(String subject) {

      this.subject = subject;
      return this;
    }

    public ScimOidcId build() {

      return new ScimOidcId(this);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((issuer == null) ? 0 : issuer.hashCode());
    result = prime * result + ((subject == null) ? 0 : subject.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ScimOidcId other = (ScimOidcId) obj;
    if (issuer == null) {
      if (other.issuer != null)
        return false;
    } else if (!issuer.equals(other.issuer))
      return false;
    if (subject == null) {
      if (other.subject != null)
        return false;
    } else if (!subject.equals(other.subject))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "ScimOidcId [issuer=" + issuer + ", subject=" + subject + "]";
  }
}
