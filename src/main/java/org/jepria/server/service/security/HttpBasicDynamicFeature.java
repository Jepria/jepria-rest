package org.jepria.server.service.security;

import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.jepria.server.data.RuntimeSQLException;
import org.jepria.server.data.sql.ConnectionContext;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;

import static org.jepria.compat.server.JepRiaServerConstant.DEFAULT_DATA_SOURCE_JNDI_NAME;
import static org.jepria.server.service.security.HttpBasic.PASSWORD;
import static org.jepria.server.service.security.HttpBasic.PASSWORD_HASH;

/**
 * Dynamic feature MUST be registered in <i>ApplicationConfig</i> for usage of <b>@HttpBasic</b> annotation.<br/>
 * Provides <b>Http Basic Authentication</b> filter for JAX-RS Adapters.<br/>
 * <b>@HttpBasic</b> annotation MUST be configured with PASSWORD/PASSWORD_HASH value for properly usage.
 */
public class HttpBasicDynamicFeature implements DynamicFeature {

  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext context) {
    final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());
    // HttpBasic annotation on the method
    HttpBasic resourceAnnotation = resourceInfo.getResourceClass().getAnnotation(HttpBasic.class);
    HttpBasic methodAnnotation = am.getAnnotation(HttpBasic.class);
    if (methodAnnotation != null) {
      context.register(new HttpBasicContainerRequestFilter(methodAnnotation.passwordType()));
      return;
    } else if (resourceAnnotation != null) {
      context.register(new HttpBasicContainerRequestFilter(resourceAnnotation.passwordType()));
      return;
    }
  }

  @Priority(Priorities.AUTHENTICATION)
  public static final class HttpBasicContainerRequestFilter  implements ContainerRequestFilter {
    
    @Context
    HttpServletRequest request;
    String passwordType;

    public HttpBasicContainerRequestFilter(String passwordType) {
      if (passwordType == null || !(PASSWORD.equals(passwordType) || PASSWORD_HASH.equals(passwordType))) {
        throw new IllegalArgumentException("Password type MUST be in (PASSWORD, PASSWORD_HASH)");
      }
      this.passwordType = passwordType;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
      String authString = requestContext.getHeaderString("authorization");
      if (authString == null) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, "Basic").build());
        return;
      }
      authString = authString.replaceFirst("[Bb]asic ", "");
      String[] credentials = new String(Base64.getDecoder().decode(authString)).split(":");
      ConnectionContext.getInstance().begin(DEFAULT_DATA_SOURCE_JNDI_NAME, "");
      Connection connection = ConnectionContext.getInstance().getConnection();
      try {
        Integer operatorId;
        if (PASSWORD.equals(passwordType)) {
          operatorId = pkg_Operator.logon(connection, credentials[0], credentials[1], null);
        } else {
          operatorId = pkg_Operator.logon(connection, credentials[0], null, credentials[1]);
        }
        requestContext.setSecurityContext(new SecurityContext(request, credentials[0], operatorId) {
          @Override
          public boolean isUserInRole(String s) {
            try {
              return super.isRole(DEFAULT_DATA_SOURCE_JNDI_NAME, s);
            } catch (SQLException ex) {
              throw new RuntimeSQLException(ex);
            }
          }
  
          @Override
          public String getAuthenticationScheme() {
            return BASIC_AUTH;
          }
        });
        ConnectionContext.getInstance().commit();
      } catch (SQLException e) {
          ConnectionContext.getInstance().rollback();
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, "Basic").build());
        return;
      } finally {
        ConnectionContext.getInstance().end();
      }
    }
  }
}
