package org.jepria.server.service.security.authorization;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Авторизация по хэш-паролю.
 */
public class AuthorizationByPasswordHash extends LoginAuthorization {
    
  /**
   * Хэш пароля
   */
  private String hash;

  AuthorizationByPasswordHash(String login, String hash) {
    super(login);
    this.hash = hash;
  }
   
  /**
   * {@inheritDoc}
   */
  @Override
  public Integer logon(Connection connection) throws SQLException {
    logger.trace("logon(Db db, " + login + ", " + hash + ")");
        
      Integer result;
      String sqlQuery = 
        " begin" 
        + "  ? := pkg_Operator.Login("
          + " operatorLogin => ?"
          + ", password => ?" 
          + ", passwordHash => ?" 
        + ");" 
        + "  ? := pkg_Operator.GetCurrentUserID;" 
        + " end;";
    try (CallableStatement callableStatement = connection.prepareCall(sqlQuery)) {
      // Установим Логин.
      callableStatement.setString(2, login);
      // Установим Пароль.
      callableStatement.setNull(3, Types.VARCHAR);
      // Установим Хэш.
      callableStatement.setString(4, hash);

      callableStatement.registerOutParameter(1, Types.VARCHAR);
      callableStatement.registerOutParameter(5, Types.INTEGER);

      callableStatement.execute();

      result = callableStatement.getInt(5);
      if (callableStatement.wasNull()) {
        result = null;
      }
    }
    return result;
  }
}
