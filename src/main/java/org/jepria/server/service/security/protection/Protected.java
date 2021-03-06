package org.jepria.server.service.security.protection;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@javax.ws.rs.NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Protected {
  String HTTP_BASIC_PASSWORD = "password";
  String HTTP_BASIC_PASSWORD_HASH = "passwordHash";
  String httpBasicPasswordType();
  boolean showOAuthLoginPage() default false;
}