package org.jepria.server.service.rest;

import org.jepria.compat.server.db.Db;
import org.jepria.oauth.sdk.GrantType;
import org.jepria.oauth.sdk.TokenRequest;
import org.jepria.oauth.sdk.TokenResponse;
import org.jepria.server.data.RuntimeSQLException;
import org.jepria.server.env.EnvironmentPropertySupport;
import org.jepria.server.service.security.oauth.OAuthDbHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

import static org.jepria.compat.server.JepRiaServerConstant.*;
import static org.jepria.oauth.sdk.OAuthConstants.*;

//TODO убрать дублирования кода
public class MetaInfoService {
  
  private final HttpServletRequest request;
  
  public MetaInfoService(HttpServletRequest request) {
    this.request = request;
  }
  
  protected String getClientId() {
    return request.getServletContext().getInitParameter(CLIENT_ID_PROPERTY);
  }
  
  private String getBackupDatasourceJndiName() {
    return EnvironmentPropertySupport.getInstance(request).getProperty(BACK_UP_DATA_SOURCE, DEFAULT_DATA_SOURCE_JNDI_NAME);
  }
  
  private String getClientSecret(String clientId) {
    String clientSecret = (String) request.getSession().getAttribute(CLIENT_SECRET_PROPERTY);
    if (clientSecret == null) {
      Db db = new Db(DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME);
      try {
        clientSecret = OAuthDbHelper.getClientSecret(db, clientId);
      } catch (SQLException ex) {
        throw new RuntimeSQLException(ex);
      } catch (Throwable ex) {
        if (ex.getMessage().contains("DataSource 'java:/comp/env/" + DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME + "' not found")) {
          db = new Db(getBackupDatasourceJndiName());
          try {
            clientSecret = OAuthDbHelper.getClientSecret(db, getClientId());
          } catch (SQLException sqlException2) {
            throw new RuntimeSQLException(sqlException2);
          }
        } else {
          throw ex;
        }
      } finally {
        db.closeAll();
      }
    }
    request.getSession().setAttribute(CLIENT_SECRET_PROPERTY, clientSecret);
    return clientSecret;
  }
  
  public TokenResponse getToken(String code) {
    String clientId = getClientId();
    String requestUri = request.getRequestURI().endsWith("/") ? request.getRequestURI().substring(0, request.getRequestURI().length() - 1) : request.getRequestURI();
    TokenRequest tokenRequest = TokenRequest.Builder()
        .resourceURI(URI.create(request.getRequestURL().toString().replaceFirst(request.getRequestURI(), OAUTH_TOKEN_CONTEXT_PATH)))
        .grantType(GrantType.AUTHORIZATION_CODE)
        .clientId(clientId)
        .clientSecret(getClientSecret(clientId))
        .redirectionURI(URI.create(requestUri))
        .authorizationCode(code)
        .build();
    TokenResponse response;
    try {
      response = tokenRequest.execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return response;
  }
  
}
