package org.jepria.server.service.security;

import oracle.jdbc.OracleTypes;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.jepria.compat.server.db.Db;

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
import java.security.Principal;
import java.sql.CallableStatement;
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

    protected Db getDb() {
      return new Db(DEFAULT_DATA_SOURCE_JNDI_NAME);
    }

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
      Db db = getDb();
      try {
        Integer operatorId;
        if (PASSWORD.equals(passwordType)) {
          operatorId = pkg_Operator.logon(db, credentials[0], credentials[1], null);
        } else {
          operatorId = pkg_Operator.logon(db, credentials[0], null, credentials[1]);
        }
        requestContext.setSecurityContext(new JerseySecurityContext(credentials[0], operatorId));
      } catch (SQLException e) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, "Basic").build());
        return;
      } finally {
        db.closeAll();
      }
    }

    final class JerseySecurityContext implements javax.ws.rs.core.SecurityContext {

      private final String username;
      private final Integer operatorId;

      public JerseySecurityContext(String username, Integer operatorId) {
        this.username = username;
        this.operatorId = operatorId;
      }

      @Override
      public boolean isUserInRole(final String roleName) {
        //language=Oracle
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
          if(callableStatement.wasNull()) result = 0;
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
        return "BASIC";
      }

      @Override
      public boolean isSecure() {
        return false;
      }
    }
  }
}
