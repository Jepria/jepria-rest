package org.jepria.server.service.security.servlet.oauth;

import org.apache.log4j.Logger;
import org.jepria.server.service.security.servlet.MultiInstanceSecurityFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;


/**
 * Фильтр для back-end сервлетов/сервисов.
 */
public class OAuthServletSecurityFilter extends MultiInstanceSecurityFilter {

  private static Logger logger = Logger.getLogger(OAuthServletSecurityFilter.class.getName());

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    super.init(filterConfig);
  }
  
  protected void prepareAccessDeniedResponse(HttpServletResponse response) {}
  
  protected void prepareUnauthorizedResponse(HttpServletResponse response) {}

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    if (isSubPath(request.getServletPath())) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }
  
    OAuthRequestWrapper oauthRequest;
    try {
      oauthRequest = request instanceof OAuthRequestWrapper ? (OAuthRequestWrapper) request : new OAuthRequestWrapper(request);
    } catch (Throwable th) {
      th.printStackTrace();
      throw new ServletException(th);
    }
  
    if (securityRoles.size() == 0 && !passAllRoles) {
      /**
       * For public resource: authorize request, if it has token. (for cases where JepMainServiceServlet is public)
       */
      String token = oauthRequest.getTokenFromRequest();
      if (token != null) {
        oauthRequest.authenticate(response);
      }
      filterChain.doFilter(oauthRequest, servletResponse);
      return;
    }

    if (oauthRequest.authenticate(response)) {
      if (securityRoles.stream().anyMatch(oauthRequest::isUserInRole) || passAllRoles) {
        filterChain.doFilter(oauthRequest, servletResponse);
      } else {
        response.sendError(SC_FORBIDDEN);
        prepareAccessDeniedResponse(response);
      }
    } else {
      response.sendError(SC_UNAUTHORIZED);
      prepareUnauthorizedResponse(response);
    }
  }

  @Override
  public void destroy() {

  }
}
