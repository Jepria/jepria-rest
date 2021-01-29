package org.jepria.server.service.security.oauth;

import oracle.jdbc.OracleTypes;
import org.jepria.server.data.RuntimeSQLException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static org.jepria.oauth.sdk.OAuthConstants.CLIENT_SECRET;

public class OAuthDbHelper {
  
  public static String getClientSecret(Connection connection, String clientId) throws NullPointerException {
    Objects.requireNonNull(clientId);
    Objects.requireNonNull(connection);
    String sqlQuery = "begin " +
      "? := pkg_OAuth.findClient(" +
        "clientShortName => ?," +
        "clientName => ''," +
        "clientNameEn => ''," +
        "maxRowCount => 1," +
        "operatorId => 1" +
      "); " +
      "end;";
    String result = null;
    try (CallableStatement cs = connection.prepareCall(sqlQuery)) {
      cs.registerOutParameter(1, OracleTypes.CURSOR);
      cs.setString(2, clientId);
      cs.executeQuery();

      //Получим набор.
      ResultSet resultSet = (ResultSet) cs.getObject(1);
      if (resultSet.next()) {
        result = resultSet.getString(CLIENT_SECRET);
      }
    } catch (SQLException ex) {
      throw new RuntimeSQLException(ex);
    }
    return result;
  }
  
}
