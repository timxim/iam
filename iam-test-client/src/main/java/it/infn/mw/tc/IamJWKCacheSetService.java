package it.infn.mw.tc;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.mitre.jose.keystore.JWKSetKeyStore;
import org.mitre.jwt.signer.service.JWTSigningAndValidationService;
import org.mitre.jwt.signer.service.impl.DefaultJWTSigningAndValidationService;
import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.jwk.JWKSet;

public class IamJWKCacheSetService extends JWKSetCacheService {

  public static final Logger LOG = LoggerFactory.getLogger(IamJWKCacheSetService.class);
  
  final ClientHttpRequestFactory requestFactory;

  final LoadingCache<String, JWTSigningAndValidationService> validators;

  public IamJWKCacheSetService(ClientHttpRequestFactory rf) {
    requestFactory = rf;
    validators = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(100)
        .build(new Fetcher(requestFactory));

  }


  public LoadingCache<String, JWTSigningAndValidationService> getValidators() {

    return validators;
  }


  @Override
  public JWTSigningAndValidationService getValidator(String jwksUri) {

    try {
      return validators.get(jwksUri);
    } catch (ExecutionException e) {
      LOG.warn(e.getMessage(),e);
      return null;
    }
  }


  private class Fetcher extends CacheLoader<String, JWTSigningAndValidationService> {

    private final RestTemplate restTemplate;

    public Fetcher(ClientHttpRequestFactory requestFactory) {
      restTemplate = new RestTemplate(requestFactory);

    }

    @Override
    public JWTSigningAndValidationService load(String key) throws Exception {

      String jsonString = restTemplate.getForObject(key, String.class);
      JWKSet jwkSet = JWKSet.parse(jsonString);

      JWKSetKeyStore keyStore = new JWKSetKeyStore(jwkSet);

      JWTSigningAndValidationService service = new DefaultJWTSigningAndValidationService(keyStore);

      return service;

    }

  }

}
