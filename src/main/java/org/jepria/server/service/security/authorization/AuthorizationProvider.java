package org.jepria.server.service.security.authorization;

import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * Абстрактный провайдер авторизации учетных записей.
 */
public abstract class AuthorizationProvider {
  
  protected static Logger logger = Logger.getLogger(AuthorizationProvider.class.getName());  
  
  /**
   * Авторизация пользователя.
   * 
   * @return    идентификатор авторизованного пользователя
   * @throws SQLException  проблема авторизации 
   */
  abstract public Integer logon() throws SQLException;
}
