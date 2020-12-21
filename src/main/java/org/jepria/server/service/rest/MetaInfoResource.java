package org.jepria.server.service.rest;

import org.jepria.oauth.sdk.TokenResponse;
import org.jepria.server.service.rest.gson.JsonConfig;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jepria.oauth.sdk.OAuthConstants.*;

@Path("/meta")
public class MetaInfoResource extends JaxrsAdapterBase {

  @GET
  @Path("/current-user")
  @JsonConfig(serializeNulls = true)
  public Response getCurrentUser() {
    Map<String, Object> responseBodyMap = new HashMap<>();
    responseBodyMap.put("username", securityContext.getCredential().getUsername());
    responseBodyMap.put("operatorId", securityContext.getCredential().getOperatorId());
    return Response.ok(responseBodyMap).build();
  }

  @GET
  @Path("/current-user/test-roles")
  public Response testCurrentUserRoles(@QueryParam("roles") String roles) {
    Map<String, Object> responseBodyMap = new LinkedHashMap<>();

    if (roles != null) {
      String[] tokens = roles.split("[~,;]"); // support '~' because it is a delimiter for a composite primary key. TODO support also both ,; delimiters?
      for (String role: tokens) {
        if (!"".equals(role)) {
          responseBodyMap.put(role, securityContext.isUserInRole(role) ? 1 : 0);
        }
      }
    }

    return Response.ok(responseBodyMap).build();
  }

  @GET
  @Path("/current-user/test-role")
  public Response testCurrentUserRole(@QueryParam("role") String role) {
    Map<String, Object> responseBodyMap = new LinkedHashMap<>();

    if (role != null && !"".equals(role)) {
      responseBodyMap.put(role, securityContext.isUserInRole(role) ? 1 : 0);
    }

    return Response.ok(responseBodyMap).build();
  }
  
  @GET
  @Path("/oauth")
  @JsonConfig(serializeNulls = true)
  public Response oauthCallback(
      @NotNull @QueryParam("code") String code,
      @QueryParam("state") String state
  ) {
    String redirectUrl = getState(request, state);
    if (redirectUrl == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    MetaInfoService service = new MetaInfoService(request);
    TokenResponse response = service.getToken(code);
    return Response
        .status(Response.Status.FOUND)
        .location(URI.create(redirectUrl))
        .cookie(new NewCookie("OAUTH_TOKEN",
            response.getAccessToken(),
            request.getContextPath(),
            null,
            1,
            null,
            -1,
            new Date(new Date().getTime() + response.getExpiresIn() * 1000),
            request.isSecure(),
            true))
        .cookie(new NewCookie(state,
            redirectUrl,
            null,
            null,
            1,
            null,
            0,
            null,
            request.isSecure(),
            true))
        .build();
  }
  
  private String getState(HttpServletRequest request, String state) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null && cookies.length > 0) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(state)) {
          cookie.setMaxAge(0);
          return cookie.getValue();
        }
      }
    }
    return null;
  }
}
