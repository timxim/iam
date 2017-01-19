package it.infn.mw.iam.test;

import java.util.ArrayList;
import java.util.List;

public class SamlIdUtils {

  public static final List<SamlId> samlIds = new ArrayList<SamlId>();

  static {

    SamlId samlId = new SamlId();
    samlId.idpId = "Saml IDP ID";
    samlId.userId = "User1 ID";
    SamlIdUtils.samlIds.add(samlId);

    samlId = new SamlId();
    samlId.idpId = "Saml IDP ID";
    samlId.userId = "User2 ID";
    SamlIdUtils.samlIds.add(samlId);

  }
}
