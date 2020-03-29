/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2019
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.infn.mw.iam.authn;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.saml.SAMLCredential;
import org.springframework.stereotype.Component;

import it.infn.mw.iam.authn.oidc.OidcExternalAuthenticationToken;
import it.infn.mw.iam.authn.saml.SamlExternalAuthenticationToken;
import it.infn.mw.iam.authn.saml.util.Saml2Attribute;

@Component
public class DefaultExternalAuthenticationInfoBuilder implements ExternalAuthenticationInfoBuilder {

  public static final String TYPE_ATTR = "type";
  public static final String OIDC_TYPE = "oidc";
  public static final String SAML_TYPE = "saml";

  public DefaultExternalAuthenticationInfoBuilder() { 
    // Empty constructor required by Spring
  }

  public Map<String, String> buildInfoMap(OidcExternalAuthenticationToken token) {
    checkNotNull(token, "token cannot be null");

    Map<String, String> result = new HashMap<>();

    result.put(TYPE_ATTR, OIDC_TYPE);
    result.put("sub", token.getExternalAuthentication().getSub());
    result.put("iss", token.getExternalAuthentication().getIssuer());

    return result;
  }

  public Map<String, String> buildInfoMap(SamlExternalAuthenticationToken token) {
    checkNotNull(token, "token cannot be null");
    Map<String, String> result = new HashMap<>();

    result.put(TYPE_ATTR, SAML_TYPE);

    SAMLCredential cred = (SAMLCredential) token.getExternalAuthentication().getCredentials();
    
    result.put("idpEntityId", cred.getRemoteEntityID());

    for (Saml2Attribute attr: Saml2Attribute.values()){
      String attrVal = cred.getAttributeAsString(attr.getAttributeName()); 
      if ( attrVal != null) {
        result.put(attr.name(), attrVal);
      }
    }

    return result;
  }

}
