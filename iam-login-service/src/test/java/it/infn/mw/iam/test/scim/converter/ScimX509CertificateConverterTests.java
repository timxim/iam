package it.infn.mw.iam.test.scim.converter;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.converter.X509CertificateConverter;
import it.infn.mw.iam.api.scim.model.ScimX509Certificate;
import it.infn.mw.iam.persistence.model.IamX509Certificate;
import it.infn.mw.iam.test.X509Utils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IamLoginService.class)
@Transactional
@WebIntegrationTest
public class ScimX509CertificateConverterTests {

  @Autowired
  X509CertificateConverter converter;

  @Test
  public void testConversionFromScimToIamWithCertificate() {

    ScimX509Certificate scimCert =
        ScimX509Certificate.builder().value(X509Utils.x509Certs.get(0).certificate).build();

    IamX509Certificate iamCert = converter.fromScim(scimCert);

    Assert.assertNull(iamCert.getLabel());
    Assert.assertFalse(iamCert.isPrimary());
    Assert.assertNotNull(iamCert.getCertificate());
    Assert.assertNotNull(iamCert.getCertificateSubject());
    Assert.assertNull(iamCert.getAccount());
  }

  @Test
  public void testConversionFromScimToIamWithCertificateAndLabel() {

    ScimX509Certificate scimCert = ScimX509Certificate.builder()
      .value(X509Utils.x509Certs.get(0).certificate)
      .display("This is the label")
      .build();

    IamX509Certificate iamCert = converter.fromScim(scimCert);

    Assert.assertEquals(iamCert.getLabel(), "This is the label");
    Assert.assertFalse(iamCert.isPrimary());
    Assert.assertNotNull(iamCert.getCertificate());
    Assert.assertNotNull(iamCert.getCertificateSubject());
    Assert.assertNull(iamCert.getAccount());
  }
}
