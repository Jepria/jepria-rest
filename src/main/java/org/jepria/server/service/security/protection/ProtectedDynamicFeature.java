package org.jepria.server.service.security.protection;

import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.jepria.server.service.rest.MetaInfoResource;
import org.jepria.server.service.security.HttpBasicDynamicFeature;
import org.jepria.server.service.security.oauth.OAuthDynamicFeature;

import javax.annotation.Priority;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

public class ProtectedDynamicFeature implements DynamicFeature {
  
  @Context
  private HttpServletRequest request;
  @Context
  private HttpServletResponse response;
  @Context
  private UriInfo uriInfo;
  private String passwordType;
  private boolean showOAuthLoginPage;
  
  public ProtectedDynamicFeature() {
    passwordType = Protected.HTTP_BASIC_PASSWORD;
    showOAuthLoginPage = false;
  }
  
  /**
   * Use this constructor to register specific HTTP Basic password type for {@link org.jepria.server.service.rest.MetaInfoResource}
   *
   * @param passwordType
   */
  public ProtectedDynamicFeature(String passwordType, boolean showOAuthLoginPage) {
    if (passwordType == null || !(Protected.HTTP_BASIC_PASSWORD.equals(passwordType) || Protected.HTTP_BASIC_PASSWORD_HASH.equals(passwordType))) {
      throw new IllegalArgumentException("Password type MUST be in (HTTP_BASIC_PASSWORD, HTTP_BASIC_PASSWORD_HASH)");
    }
    this.passwordType = passwordType;
    this.showOAuthLoginPage = showOAuthLoginPage;
  }
  
  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext context) {
    final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());
    // HttpBasic annotation on the method
    Protected resourceAnnotation = resourceInfo.getResourceClass().getAnnotation(Protected.class);
    Protected methodAnnotation = am.getAnnotation(Protected.class);
    if (methodAnnotation != null) {
      context.register(new ProtectedRequestFilter(methodAnnotation.httpBasicPasswordType(), methodAnnotation.showOAuthLoginPage()));
      return;
    } else if (resourceAnnotation != null) {
      context.register(new ProtectedRequestFilter(resourceAnnotation.httpBasicPasswordType(), resourceAnnotation.showOAuthLoginPage()));
      return;
    } else if (MetaInfoResource.class.equals(resourceInfo.getResourceClass()) && resourceInfo.getResourceMethod().getName() != "oauthCallback") {
      context.register(new ProtectedRequestFilter(passwordType, showOAuthLoginPage));
    }
  }
  
  @Priority(Priorities.AUTHENTICATION)
  public final class ProtectedRequestFilter implements ContainerRequestFilter {
    
    HttpBasicDynamicFeature.HttpBasicContainerRequestFilter httpBasicContainerRequestFilter;
    OAuthDynamicFeature.OAuthContainerRequestFilterImpl OAuthContainerRequestFilter;
    
    public ProtectedRequestFilter(String passwordType, boolean showOAuthLoginPage) {
      this.httpBasicContainerRequestFilter = new HttpBasicDynamicFeature.HttpBasicContainerRequestFilter(passwordType);
      this.OAuthContainerRequestFilter = new OAuthDynamicFeature.OAuthContainerRequestFilterImpl(request, response, uriInfo, showOAuthLoginPage);
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
