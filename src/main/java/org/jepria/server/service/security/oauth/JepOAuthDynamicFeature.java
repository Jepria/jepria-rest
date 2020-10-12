package org.jepria.server.service.security.oauth;

import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.jepria.compat.server.db.Db;
import org.jepria.oauth.sdk.TokenInfoResponse;
import org.jepria.oauth.sdk.jaxrs.OAuthContainerRequestFilter;
import org.jepria.server.data.RuntimeSQLException;
import org.jepria.server.env.EnvironmentPropertySupport;
import org.jepria.server.service.security.SecurityContext;

import javax.annotation.Priority;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import java.io.IOException;
import java.sql.SQLException;

import static org.jepria.compat.server.JepRiaServerConstant.*;
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
      return new SecurityContext(request, credentials[0], Integer.valueOf(credentials[1])) {

        @Override
        public String getAuthenticationScheme() {
          return AUTHENTICATION_SCHEME;
        }

        @Override
        public boolean isUserInRole(String s) {
          Db db = new Db(DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME);
          try {
            return super.isRole(db, s);
          } catch (SQLException sqlException1) {
            throw new RuntimeSQLException(sqlException1);
          } catch (Throwable ex) {
            if (ex.getMessage().contains("DataSource 'java:/comp/env/" + DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME + "' not found")) {
              ex.printStackTrace();
              db = new Db(DEFAULT_DATA_SOURCE_JNDI_NAME);
              try {
                return super.isRole(db, s);
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
      };
    }

    @Override
    protected HttpServletRequest getRequest() {
      return request;
    }

    private String getBackupDatasourceJndiName() {
      return EnvironmentPropertySupport.getInstance(request).getProperty(BACK_UP_DATA_SOURCE, DEFAULT_DATA_SOURCE_JNDI_NAME);
    }

    @Override
    protected String getClientSecret() {
      String clientSecret = (String) request.getSession().getAttribute(CLIENT_SECRET_PROPERTY);
      if (clientSecret == null) {
        Db db = new Db(DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME);
        try {
          clientSecret = OAuthDbHelper.getClientSecret(db, getClientId());
        } catch (SQLException ex) {
          throw new RuntimeSQLException(ex);
        } catch (Throwable ex) {
          if (ex.getMessage().contains("DataSource 'java:/comp/env/" + DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME + "' not found")) {
            ex.printStackTrace();
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

    @Override
    protected String getClientId() {
      return request.getServletContext().getInitParameter(CLIENT_ID_PROPERTY);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
      super.filter(containerRequestContext);
    }
  }
}
