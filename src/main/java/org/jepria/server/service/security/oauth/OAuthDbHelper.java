package org.jepria.server.service.security.oauth;

import org.jepria.compat.server.db.Db;
import org.jepria.server.data.RuntimeSQLException;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static org.jepria.oauth.sdk.OAuthConstants.CLIENT_SECRET;

public class OAuthDbHelper {
  
  public static String getClientSecret(Db db, String clientId) throws SQLException, NullPointerException {
    Objects.requireNonNull(clientId);
    Objects.requireNonNull(db);
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
    CallableStatement cs = db.prepare(sqlQuery);
    try {
      cs.setString(1, clientId);
      ResultSet rs = cs.executeQuery();
      if (rs.next()) {
        result = rs.getString(CLIENT_SECRET);
      }
    } catch (SQLException ex) {
      throw new RuntimeSQLException(ex);
    } finally {
      cs.close();
    }
    return result;
  }
  
}
