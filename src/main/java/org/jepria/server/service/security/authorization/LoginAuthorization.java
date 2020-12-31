package org.jepria.server.service.security.authorization;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Авторизация по логину.
 */
public class LoginAuthorization extends AuthorizationProvider {
  
  /**
   * Логин учетной записи
   */
  protected String login;

  LoginAuthorization(String login) {
    this.login = login;
  }
    
  /**
   * {@inheritDoc}
   */
  @Override
  public Integer logon(Connection connection) throws SQLException {
      logger.trace("logon(Db db, " + login + ")");
      
      Integer result;
      String sqlQuery = 
        " begin" 
        + "  ? := pkg_Operator.Login(" 
            + "operatorLogin => ?" 
        + "  );" 
        + "  ? := pkg_Operator.GetCurrentUserID;" 
        + " end;";
      try(CallableStatement callableStatement = connection.prepareCall(sqlQuery)) {
        
        // Установим Логин.
        callableStatement.setString(2, login); 

        callableStatement.registerOutParameter(1, Types.VARCHAR);
        callableStatement.registerOutParameter(3, Types.INTEGER);

        callableStatement.execute();

        result = callableStatement.getInt(3);
        if(callableStatement.wasNull()) {
          result = null;
        }
      }

      return result;
  }
}
