package org.jepria.server.service.security.authorization;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Парольная авторизация.
 */
public class AuthorizationByPassword extends LoginAuthorization {
    
  /**
   * Пароль учетной записи
   */
  private String password;

  AuthorizationByPassword(String login, String password) {
    super(login);
    this.password = password;
  }
   
  /**
   * {@inheritDoc}
   */
  @Override
  public Integer logon(Connection connection) throws SQLException {
    logger.trace("logon(Db db, " + login + ", " + password + ")");
      
      Integer result = null;
      String sqlQuery = 
        " begin" 
        + "  ? := pkg_Operator.Login("
          + " operatorLogin => ?"
          + ", password => ?"
        + ");" 
        + "  ? := pkg_Operator.GetCurrentUserID;" 
        + " end;";
    try (CallableStatement callableStatement = connection.prepareCall(sqlQuery)) {
      // Установим Логин.
      callableStatement.setString(2, login);
      // Установим Пароль.
      callableStatement.setString(3, password);

      callableStatement.registerOutParameter(1, Types.VARCHAR);
      callableStatement.registerOutParameter(4, Types.INTEGER);

      callableStatement.execute();

      result = callableStatement.getInt(4);
      if (callableStatement.wasNull())
        result = null;

    }

    return result;
  }
    
}
