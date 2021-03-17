package org.jepria.server.service.security.oauth;

import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.jepria.compat.server.db.Db;
import org.jepria.oauth.sdk.*;
import org.jepria.oauth.sdk.jaxrs.OAuthContainerRequestFilter;
import org.jepria.server.data.RuntimeSQLException;
import org.jepria.server.env.EnvironmentPropertySupport;
import org.jepria.server.service.security.SecurityContext;
import org.jepria.server.service.security.protection.Protected;

import javax.annotation.Priority;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

import static org.jepria.compat.server.JepRiaServerConstant.*;
import static org.jepria.oauth.sdk.OAuthConstants.*;
import static org.jepria.oauth.sdk.OAuthConstants.OAUTH_AUTHORIZATION_CONTEXT_PATH;

public class OAuthDynamicFeature implements DynamicFeature {
  
  
  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final UriInfo uriInfo;
  
  public OAuthDynamicFeature(@Context HttpServletRequest request,
                                @Context HttpServletResponse response,
                                @Context UriInfo uriInfo) {
    this.request = request;
    this.response = response;
    this.uriInfo = uriInfo;
  }
  
  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext context) {
    
    final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());
    // HttpBasic annotation on the method
    OAuth resourceAnnotation = resourceInfo.getResourceClass().getAnnotation(OAuth.class);
    OAuth methodAnnotation = am.getAnnotation(OAuth.class);
    if (methodAnnotation != null) {
      context.register(new OAuthContainerRequestFilterImpl(request, response, uriInfo, methodAnnotation.showLoginPage()));
      return;
    } else if (resourceAnnotation != null) {
      context.register(new OAuthContainerRequestFilterImpl(request, response, uriInfo, resourceAnnotation.showLoginPage()));
      return;
    }
  }
  
  @Priority(Priorities.AUTHENTICATION)
  public static class OAuthContainerRequestFilterImpl extends OAuthContainerRequestFilter {
    
    public static final String AUTHENTICATION_SCHEME = "BEARER";
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final UriInfo uriInfo;
    private final boolean showLoginPage;
    
    public OAuthContainerRequestFilterImpl(@Context HttpServletRequest request,
                                           @Context HttpServletResponse response,
                                           @Context UriInfo uriInfo,
                                           boolean showLoginPage) {
      this.request = request;
      this.response = response;
      this.uriInfo = uriInfo;
      this.showLoginPage = showLoginPage;
    }
    
    @Override
    protected String getTokenFromCookie() {
      String tokenString = null;
      Cookie[] cookies = request.getCookies();
      if (cookies == null) {
        return null;
      }
      for (Cookie cookie : cookies) {
        if (cookie.getName().equalsIgnoreCase("OAUTH_TOKEN")) {
          tokenString = cookie.getValue();
          break;
        }
      }
      if (request.getSession().getAttribute("OAUTH_TOKEN") != null) {
        tokenString = (String) request.getSession().getAttribute("OAUTH_TOKEN");
      }
      return tokenString;
    }
    
    private TokenInfoResponse getTokenInfo(String tokenString) throws IOException {
      TokenInfoRequest tokenInfoRequest = TokenInfoRequest.Builder()
          .resourceURI(URI.create(getRequest().getRequestURL().toString().replaceFirst(getRequest().getRequestURI(), OAUTH_TOKENINFO_CONTEXT_PATH)))
          .clientId(getClientId())
          .clientSecret(getClientSecret())
          .token(tokenString)
          .build();
      TokenInfoResponse response = tokenInfoRequest.execute();
      return response;
    }
    
    protected void authorizationRequest(HttpServletResponse httpServletResponse, ContainerRequestContext containerRequestContext) {
      try {
        /**
         * Create state param and save it to Cookie for checking in future to prevent 'Replay attacks'
         */
        String redirectUri;
        if (request.getQueryString() != null && request.getQueryString().length() > 0) {
          redirectUri = request.getRequestURL().append("?").append(request.getQueryString()).toString();
        } else {
          redirectUri = request.getRequestURL().toString();
        }
        State state = new State();
        Cookie stateCookie = new Cookie(state.toString(), redirectUri);
        stateCookie.setSecure(request.isSecure());
        stateCookie.setPath(request.getContextPath());
        stateCookie.setHttpOnly(true);
        httpServletResponse.addCookie(stateCookie);
        
        String authorizationRequestURI = AuthorizationRequest.Builder()
            .resourceURI(URI.create(request.getRequestURL().toString().replaceFirst(request.getRequestURI(), OAUTH_AUTHORIZATION_CONTEXT_PATH)))
            .responseType(ResponseType.CODE)
            .clientId(getClientId())
            .redirectionURI(URI.create(uriInfo.getBaseUri().getPath() + "meta/oauth"))
            .state(state)
            .build()
            .toString();
        
        containerRequestContext.abortWith(Response.status(Response.Status.FOUND).location(URI.create(authorizationRequestURI)).build());
      } catch (Throwable e) {
        e.printStackTrace();
      }
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
      if (showLoginPage) {
        /*
         * Get token from Authorization Header
         */
        String tokenString = getTokenFromHeader();
        if (tokenString != null) {
          super.filter(containerRequestContext);
        } else {
          tokenString = getTokenFromCookie();
          if (tokenString == null) {
            authorizationRequest(response, containerRequestContext);
          } else {
            TokenInfoResponse tokenInfoResponse = getTokenInfo(tokenString);
            if (!tokenInfoResponse.getActive()) {
              authorizationRequest(response, containerRequestContext);
            } else {
              containerRequestContext.setSecurityContext(getSecurityContext(tokenInfoResponse));
            }
          }
        }
      } else {
        super.filter(containerRequestContext);
      }
    }
  }
}
