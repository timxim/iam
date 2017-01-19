package it.infn.mw.iam.test;

import java.util.ArrayList;
import java.util.List;

public class OidcIdUtils {

  public static final List<OidcId> oidcIds = new ArrayList<OidcId>();

  static {

    OidcId oidcId = new OidcId();
    oidcId.issuer = "Oidc ID Issuer";
    oidcId.subject = "User1 subject";
    oidcIds.add(oidcId);

    oidcId = new OidcId();
    oidcId.issuer = "Oidc ID Issuer";
    oidcId.subject = "User2 subject";
    oidcIds.add(oidcId);

  }
}
