package org.jepria.server.service.security.protection;

import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.jepria.server.service.rest.MetaInfoResource;
import org.jepria.server.service.security.HttpBasicDynamicFeature;
import org.jepria.server.service.security.oauth.JepOAuthDynamicFeature;

import javax.annotation.Priority;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class ProtectedDynamicFeature implements DynamicFeature {

  @Context
  HttpServletRequest request;

  String passwordType;

  public ProtectedDynamicFeature() {
    passwordType = Protected.HTTP_BASIC_PASSWORD;
  }

  /**
   * Use this constructor to register specific HTTP Basic password type for {@link org.jepria.server.service.rest.MetaInfoResource}
   * @param passwordType
   */
  public ProtectedDynamicFeature(String passwordType) {
    if (passwordType == null || !(Protected.HTTP_BASIC_PASSWORD.equals(passwordType) || Protected.HTTP_BASIC_PASSWORD_HASH.equals(passwordType))) {
      throw new IllegalArgumentException("Password type MUST be in (HTTP_BASIC_PASSWORD, HTTP_BASIC_PASSWORD_HASH)");
    }
    this.passwordType = passwordType;
  }

  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext context) {
    final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());
    // HttpBasic annotation on the method
    Protected resourceAnnotation = resourceInfo.getResourceClass().getAnnotation(Protected.class);
    Protected methodAnnotation = am.getAnnotation(Protected.class);
    if (methodAnnotation != null) {
      context.register(new ProtectedRequestFilter(methodAnnotation.httpBasicPasswordType()));
      return;
    } else if (resourceAnnotation != null) {
      context.register(new ProtectedRequestFilter(resourceAnnotation.httpBasicPasswordType()));
      return;
    } else if (MetaInfoResource.class.equals(resourceInfo.getResourceClass())) {
      context.register(new ProtectedRequestFilter(passwordType));
    }
  }

  @Priority(Priorities.AUTHENTICATION)
  public final class ProtectedRequestFilter implements ContainerRequestFilter {

    HttpBasicDynamicFeature.HttpBasicContainerRequestFilter httpBasicContainerRequestFilter;
    JepOAuthDynamicFeature.JepOAuthContainerRequestFilter OAuthContainerRequestFilter;

    public ProtectedRequestFilter(String passwordType) {
      this.httpBasicContainerRequestFilter = new HttpBasicDynamicFeature.HttpBasicContainerRequestFilter(passwordType);
      this.OAuthContainerRequestFilter = new JepOAuthDynamicFeature.JepOAuthContainerRequestFilter(request);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
      String authString = containerRequestContext.getHeaderString("authorization");
      if (authString == null) {
        containerRequestContext.abortWith(Response.status((Response.Status.UNAUTHORIZED)).build());
      } else if (authString.startsWith("Basic") || authString.startsWith("basic")) {
        httpBasicContainerRequestFilter.filter(containerRequestContext);
      } else if (authString.startsWith("Bearer") || authString.startsWith("bearer")) {
        OAuthContainerRequestFilter.filter(containerRequestContext);
      } else {
        containerRequestContext.abortWith(Response.status((Response.Status.UNAUTHORIZED)).build());
      }
    }

  }
}
