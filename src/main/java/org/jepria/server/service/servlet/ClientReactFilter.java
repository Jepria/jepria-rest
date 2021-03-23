package org.jepria.server.service.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

public class ClientReactFilter implements Filter {
  
  private String reactPath = "/ui/index.html";
  
  public void init(FilterConfig filterConfig) {
    String initParam = filterConfig.getInitParameter("path");
    if (initParam != null && initParam.length() > 0) {
      reactPath = initParam;
    }
  }
  
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    String requestPath = ((HttpServletRequest) servletRequest).getRequestURI();
    if (!requestPath.contains(".")) {
      if (requestPath.endsWith("/")) {
        File file = new File(requestPath + "index.html");
        if (file.exists()) {
          filterChain.doFilter(servletRequest, servletResponse);
          return;
        }
      } else {
        File file = new File(requestPath + ".html");
        if (file.exists()) {
          filterChain.doFilter(servletRequest, servletResponse);
          return;
        }
      }
    } else {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }
    servletRequest.getServletContext().getRequestDispatcher(reactPath).forward(servletRequest, servletResponse);
    return;
  }
  
  public void destroy() {
    reactPath = null;
  }
}
