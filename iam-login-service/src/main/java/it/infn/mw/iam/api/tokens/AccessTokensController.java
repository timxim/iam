/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2018
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
package it.infn.mw.iam.api.tokens;

import static it.infn.mw.iam.api.tokens.Constants.ACCESS_TOKENS_ENDPOINT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import it.infn.mw.iam.api.common.ErrorDTO;
import it.infn.mw.iam.api.common.ListResponseDTO;
import it.infn.mw.iam.api.tokens.exception.TokenNotFoundException;
import it.infn.mw.iam.api.tokens.model.AccessToken;
import it.infn.mw.iam.api.tokens.service.TokenService;
import it.infn.mw.iam.api.tokens.service.paging.TokensPageRequest;
import it.infn.mw.iam.core.user.exception.IamAccountException;

@RestController
@Transactional
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(ACCESS_TOKENS_ENDPOINT)
public class AccessTokensController
    extends AbstractTokensController<AccessToken, OAuth2AccessTokenEntity> {

  @Autowired
  private TokenService<OAuth2AccessTokenEntity> tokenService;

  @RequestMapping(method = GET, produces = APPLICATION_JSON_CONTENT_TYPE)
  public MappingJacksonValue listAccessTokens(
      @RequestParam(required = false, defaultValue = "1") Integer startIndex,
      @RequestParam(required = false, defaultValue = "" + TOKENS_MAX_PAGE_SIZE) Integer count,
      @RequestParam(required = false) String userId,
      @RequestParam(required = false) String clientId,
      @RequestParam(required = false, defaultValue = "expiration") String sortBy,
      @RequestParam(required = false, defaultValue = "desc") String sortDirection,
      @RequestParam(required = false) final String attributes) {

    TokensPageRequest pageRequest =
        buildTokensPageRequest(startIndex, count, clientId, userId, sortBy, sortDirection);
    ListResponseDTO<AccessToken> results = getResponse(pageRequest);
    return filterAttributes(results, attributes);
  }

  @RequestMapping(method = DELETE)
  @ResponseStatus(NO_CONTENT)
  public void deleteAllTokens() {

    tokenService.deleteAllTokens();
  }

  @RequestMapping(method = GET, value = "/{id}", produces = APPLICATION_JSON_CONTENT_TYPE)
  public AccessToken getAccessToken(@PathVariable("id") Long id) {

    return buildTokenResponse(
        tokenService.getTokenById(id).orElseThrow(() -> new TokenNotFoundException(id)));
  }

  @RequestMapping(method = DELETE, value = "/{id}")
  @ResponseStatus(NO_CONTENT)
  public void revokeAccessToken(@PathVariable("id") Long id) {

    tokenService.revokeTokenById(id);
  }

  @ResponseStatus(value = NOT_FOUND)
  @ExceptionHandler(TokenNotFoundException.class)
  public ErrorDTO tokenNotFoundError(Exception ex) {

    return ErrorDTO.fromString(ex.getMessage());
  }

  @ResponseStatus(value = INTERNAL_SERVER_ERROR)
  @ExceptionHandler({IamAccountException.class, InvalidClientException.class})
  public ErrorDTO accountNotFoundError(Exception ex) {

    return ErrorDTO.fromString(ex.getMessage());
  }
}
