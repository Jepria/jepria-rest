package org.jepria.server.service.security.servlet.oauth;

import oracle.jdbc.OracleTypes;
import org.apache.log4j.Logger;
import org.jepria.compat.server.db.Db;
import org.jepria.oauth.sdk.*;
import org.jepria.server.data.RuntimeSQLException;
import org.jepria.server.env.EnvironmentPropertySupport;
import org.jepria.server.service.security.oauth.OAuthDbHelper;
import org.jepria.ssoutils.JepPrincipal;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.sql.CallableStatement;
import java.sql.SQLException;

import static org.jepria.compat.server.JepRiaServerConstant.*;
import static org.jepria.oauth.sdk.OAuthConstants.*;

public class OAuthRequestWrapper extends HttpServletRequestWrapper {
  
  private static Logger logger = Logger.getLogger(OAuthRequestWrapper.class.getName());
  public static final String OAUTH_TOKEN = "OAUTH_TOKEN";
  public static final String AUTH_TYPE = "Bearer";
  
  protected HttpServletRequest delegate;
  private String tokenString = null;
  private JepPrincipal principal;
  private final String clientId;
  private final String clientSecret;
  
  public OAuthRequestWrapper(HttpServletRequest request) throws RuntimeException {
    super(request);
    delegate = request;
    clientId = delegate.getServletContext().getInitParameter(CLIENT_ID_PROPERTY);
    clientSecret = getClientSecret();
  }
  
  private String getBackupDatasourceJndiName() {
    return EnvironmentPropertySupport.getInstance(delegate).getProperty(BACK_UP_DATA_SOURCE, DEFAULT_DATA_SOURCE_JNDI_NAME);
  }
  
  /**
   * Request token from OAuth server
   */
  public TokenResponse getToken(String code) throws IOException {
    logger.trace("BEGIN getToken()");
    TokenRequest tokenRequest = TokenRequest.Builder()
      .resourceURI(URI.create(delegate.getRequestURL().toString().replaceFirst(delegate.getRequestURI(), OAUTH_TOKEN_CONTEXT_PATH)))
      .grantType(GrantType.AUTHORIZATION_CODE)
      .clientId(clientId)
      .clientSecret(clientSecret)
      .redirectionURI(URI.create(delegate.getContextPath()))
      .authorizationCode(code)
      .build();
    TokenResponse response = tokenRequest.execute();
    logger.trace("END getToken()");
    return response;
  }
  
  public void buildAuthorizationRequest(HttpServletResponse httpServletResponse) {
    logger.trace("BEGIN buildAuthorizationRequest()");
    try {
      /**
       * Create state param and save it to Cookie for checking in future to prevent 'Replay attacks'
       */
      String redirectUri;
      if (delegate.getQueryString() != null && delegate.getQueryString().length() > 0) {
        redirectUri = delegate.getRequestURL().append("?").append(delegate.getQueryString()).toString();
      } else {
        redirectUri = delegate.getRequestURL().toString();
      }
      State state = new State();
      Cookie stateCookie = new Cookie(state.toString(), redirectUri);
      stateCookie.setSecure(delegate.isSecure());
      stateCookie.setPath(delegate.getContextPath());
      stateCookie.setHttpOnly(true);
      httpServletResponse.addCookie(stateCookie);
      
      String authorizationRequestURI = AuthorizationRequest.Builder()
        .resourceURI(URI.create(delegate.getRequestURL().toString().replaceFirst(delegate.getRequestURI(), OAUTH_AUTHORIZATION_CONTEXT_PATH)))
        .responseType(ResponseType.CODE)
        .clientId(clientId)
        .redirectionURI(URI.create(delegate.getContextPath()))
        .state(state)
        .build()
        .toString();
      
      httpServletResponse.sendRedirect(authorizationRequestURI);
    } catch (Throwable e) {
      e.printStackTrace();
    }
    logger.trace("END buildAuthorizationRequest()");
  }
  
  protected String getClientSecret() throws RuntimeSQLException {
    String clientSecret = (String) delegate.getSession().getAttribute(CLIENT_SECRET_PROPERTY);
    if (clientSecret == null) {
      Db db = new Db(DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME);
      try {
        clientSecret = OAuthDbHelper.getClientSecret(db, clientId);
      } catch (SQLException ex) {
        throw new RuntimeSQLException(ex);
      } catch (Throwable ex) {
        if (ex.getMessage().contains("DataSource 'java:/comp/env/" + DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME + "' not found")) {
          ex.printStackTrace();
          db = new Db(getBackupDatasourceJndiName());
          try {
            clientSecret = OAuthDbHelper.getClientSecret(db, clientId);
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
    delegate.getSession().setAttribute(CLIENT_SECRET_PROPERTY, clientSecret);
    return clientSecret;
  }
  
  public String getTokenFromRequest() {
    if (tokenString != null) {
      return tokenString;
    }
    Cookie[] cookies = delegate.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (cookie.getName().equalsIgnoreCase(OAUTH_TOKEN)) {
        tokenString = cookie.getValue();
        break;
      }
    }
    if (tokenString == null) {
      String headerText = delegate.getHeader("Authorization");
      if (headerText != null && headerText.startsWith("Bearer")) {
        tokenString = headerText.replaceFirst("Bearer ", "");
      } else {
        tokenString = null;
      }
    }
    if (delegate.getSession().getAttribute(OAUTH_TOKEN) != null) {
      tokenString = (String) delegate.getSession().getAttribute(OAUTH_TOKEN);
    }
    return tokenString;
  }
  
  @Override
  public String getAuthType() {
    return AUTH_TYPE;
  }
  
  @Override
  public boolean isUserInRole(String role) {
    logger.trace("BEGIN isUserInRole()");
    Db db = new Db(DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME);
    try {
      return isRole(db, role);
    } catch (SQLException sqlException1) {
      sqlException1.printStackTrace();
    } catch (Throwable ex) {
      if (ex.getMessage().contains("DataSource 'java:/comp/env/" + DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME + "' not found")) {
        ex.printStackTrace();
        db = new Db(getBackupDatasourceJndiName());
        try {
          return isRole(db, role);
        } catch (SQLException sqlException2) {
          sqlException2.printStackTrace();
        }
      } else {
        throw ex;
      }
    } finally {
      db.closeAll();
    }
    return false;
  }
  
  protected boolean isRole(Db db, String roleShortName) throws SQLException {
    String sqlQuery =
      "begin ? := pkg_operator.isrole(" +
        "operatorid => ?, " +
        "roleshortname => ?" +
        "); " +
        "end;";
    int result;
    try (CallableStatement callableStatement = db.prepare(sqlQuery)) {
      callableStatement.registerOutParameter(1, OracleTypes.INTEGER);
      callableStatement.setInt(2, principal.getOperatorId());
      callableStatement.setString(3, roleShortName);
      callableStatement.execute();
      result = new Integer(callableStatement.getInt(1));
      if (callableStatement.wasNull()) result = 0;
    }
    return result == 1;
  }
  
  @Override
  public Principal getUserPrincipal() {
    return principal;
  }
  
  /**
   * Request token information form OAuth server
   */
  private TokenInfoResponse getTokenInfo() throws IOException {
    logger.trace("BEGIN getTokenInfo()");
    TokenInfoRequest request = TokenInfoRequest.Builder()
      .resourceURI(URI.create(delegate.getRequestURL().toString().replaceFirst(delegate.getRequestURI(), OAUTH_TOKENINFO_CONTEXT_PATH)))
      .clientId(clientId)
      .clientSecret(clientSecret)
      .token(tokenString)
      .build();
    TokenInfoResponse response = request.execute();
    logger.trace("END getTokenInfo()");
    return response;
  }
  
  @Override
  public boolean authenticate(HttpServletResponse httpServletResponse) {
    logger.trace("BEGIN authenticate()");
    try {
      String tokenString = getTokenFromRequest();
      if (tokenString == null) {
        logger.trace("ERROR authenticate() - token not found");
        return false;
      }
      TokenInfoResponse tokenClaims = getTokenInfo();
      if (tokenClaims != null && tokenClaims.getActive()) {
        String subject = tokenClaims.getSub();
        String username = tokenClaims.getUsername();
        if (subject.contains(":")) {
          String[] credentials = tokenClaims.getSub().split(":");
          username = credentials[0];
          subject = credentials[1];
        }
        principal = new JepPrincipal(username, Integer.valueOf(subject));
      } else {
        logger.trace("ERROR authenticate() - token is invalid");
        Cookie[] cookies = delegate.getCookies();
        for (Cookie cookie : cookies) {
          if (cookie.getName().equalsIgnoreCase(OAUTH_TOKEN)) {
            logger.trace("TRACE authenticate() - deleting token cookie");
            Cookie deletedCookie = new Cookie(cookie.getName(), cookie.getValue());
            deletedCookie.setMaxAge(0);
            deletedCookie.setPath("/");
            deletedCookie.setHttpOnly(true);
            httpServletResponse.addCookie(deletedCookie);
          }
        }
        return false;
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return false;
    }
    logger.trace("END authenticate()");
    return principal != null;
  }
  
  @Override
  public void login(String username, String password) throws ServletException {
    TokenRequest tokenRequest = TokenRequest.Builder()
      .resourceURI(URI.create(delegate.getRequestURL().toString().replaceFirst(delegate.getRequestURI(), OAUTH_TOKEN_CONTEXT_PATH)))
      .clientId(clientId)
      .clientSecret(clientSecret)
      .grantType(GrantType.PASSWORD)
      .userName(username)
      .password(password)
      .build();
    try {
      TokenResponse tokenResponse = tokenRequest.execute();
      delegate.getSession().setAttribute(OAUTH_TOKEN, tokenResponse.getAccessToken());
    } catch (IOException e) {
      e.printStackTrace();
      throw new ServletException(e);
    }
  }
  
  @Override
  public void logout() throws ServletException {
    String tokenString = getTokenFromRequest();
    if (tokenString != null) {
      TokenRevocationRequest request = TokenRevocationRequest.Builder()
        .resourceURI(URI.create(delegate.getRequestURL().toString().replaceFirst(delegate.getRequestURI(), OAUTH_TOKENREVOKE_CONTEXT_PATH)))
        .token(tokenString)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .build();
      try {
        request.execute();
      } catch (IOException e) {
        e.printStackTrace();
        throw new ServletException(e);
      }
    } else {
      delegate.logout();
      return;
    }
  }
  
}
