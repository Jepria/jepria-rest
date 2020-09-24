package org.jepria.server.service.security.oauth;

import oracle.jdbc.OracleTypes;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.jepria.compat.server.db.Db;
import org.jepria.oauth.sdk.TokenInfoResponse;
import org.jepria.oauth.sdk.jaxrs.OAuthContainerRequestFilter;
import org.jepria.server.data.RuntimeSQLException;
import org.jepria.server.service.security.PrincipalImpl;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import java.io.IOException;
import java.security.Principal;
import java.sql.CallableStatement;
import java.sql.SQLException;

import static org.jepria.compat.server.JepRiaServerConstant.DEFAULT_DATA_SOURCE_JNDI_NAME;
import static org.jepria.oauth.sdk.OAuthConstants.CLIENT_ID_PROPERTY;
import static org.jepria.oauth.sdk.OAuthConstants.CLIENT_SECRET_PROPERTY;

public class JepOAuthDynamicFeature implements DynamicFeature {

  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext context) {

    final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());
    // HttpBasic annotation on the method
    OAuth resourceAnnotation = resourceInfo.getResourceClass().getAnnotation(OAuth.class);
    OAuth methodAnnotation = am.getAnnotation(OAuth.class);
    if (methodAnnotation != null) {
      context.register(JepOAuthContainerRequestFilter.class);
      return;
    } else if (resourceAnnotation != null) {
      context.register(JepOAuthContainerRequestFilter.class);
      return;
    }
  }

  @Priority(Priorities.AUTHENTICATION)
  public static class JepOAuthContainerRequestFilter extends OAuthContainerRequestFilter {

    public static final String AUTHENTICATION_SCHEME = "BEARER";
    HttpServletRequest request;

    public JepOAuthContainerRequestFilter(@Context HttpServletRequest request) {
      this.request = request;
    }

    @Override
    protected SecurityContext getSecurityContext(TokenInfoResponse tokenInfo) {
      String[] credentials = tokenInfo.getSub().split(":");
      return new SecurityContext(request, credentials[0], Integer.valueOf(credentials[1]));
    }

    @Override
    protected HttpServletRequest getRequest() {
      return request;
    }

    @Override
    protected String getClientSecret() {
      String clientSecret = (String) request.getSession().getAttribute(CLIENT_SECRET_PROPERTY);
      if (clientSecret == null) {
        Db db = new Db(DEFAULT_DATA_SOURCE_JNDI_NAME);
        try {
          clientSecret = OAuthDbHelper.getClientSecret(db, getClientId());
        } catch (SQLException ex) {
          ex.printStackTrace();
          throw new RuntimeSQLException(ex);
        }
      }
      request.getSession().setAttribute(CLIENT_SECRET_PROPERTY, clientSecret);
      return clientSecret;
    }

    @Override
    protected String getClientId() {
      return request.getServletContext().getInitParameter(CLIENT_ID_PROPERTY);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
      super.filter(containerRequestContext);
    }
  }

  private static final class SecurityContext implements javax.ws.rs.core.SecurityContext {

    private Db getDb() {
      return new Db(DEFAULT_DATA_SOURCE_JNDI_NAME);
    }

    private final String username;
    private final Integer operatorId;
    private HttpServletRequest request;

    public SecurityContext(HttpServletRequest request, String username, Integer operatorId) {
      this.request = request;
      this.username = username;
      this.operatorId = operatorId;
    }

    @Override
    public boolean isUserInRole(final String roleName) {
      String sqlQuery =
          "begin ? := pkg_operator.isrole(" +
              "operatorid => ?, " +
              "roleshortname => ?" +
              "); " +
              "end;";
      Db db = getDb();
      int result = 0;
      try {
        CallableStatement callableStatement = db.prepare(sqlQuery);
        callableStatement.registerOutParameter(1, OracleTypes.INTEGER);
        callableStatement.setInt(2, operatorId);
        callableStatement.setString(3, roleName);
        callableStatement.execute();
        result = new Integer(callableStatement.getInt(1));
        if (callableStatement.wasNull()) result = 0;
      } catch (SQLException e) {
        e.printStackTrace();
      } finally {
        db.closeAll();
      }
      return result == 1;
    }

    @Override
    public Principal getUserPrincipal() {
      return new PrincipalImpl(username, operatorId);
    }

    @Override
    public String getAuthenticationScheme() {
      return JepOAuthContainerRequestFilter.AUTHENTICATION_SCHEME;
    }

    @Override
    public boolean isSecure() {
      return request.isSecure();
    }
  }
}
