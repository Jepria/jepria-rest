package org.jepria.server.service.security.oauth;

import oracle.jdbc.OracleTypes;
import org.jepria.compat.server.db.Db;
import org.jepria.server.data.DaoSupport;
import org.jepria.server.data.RuntimeSQLException;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
    } finally {
      cs.close();
    }
    return result;
  }
  
  public static Integer loginByClientSecret(Db db, String clientId, String clientSecret) {
    Objects.requireNonNull(clientId);
    Objects.requireNonNull(clientSecret);
    Objects.requireNonNull(db);
    String sqlQuery =
      "begin  "
        + "? := pkg_OAuth.verifyClientCredentials("
        + "clientShortName => ? "
        + ", clientSecret => ? "
        + ");"
        + " end;";
    Integer result;
    CallableStatement cs = db.prepare(sqlQuery);
    try {
      cs.registerOutParameter(1, Types.INTEGER);
      cs.setString(2, clientId);
      cs.setString(3, clientSecret);
      cs.executeQuery();
      result = cs.getInt(1);
    } catch (Throwable th) {
      throw new RuntimeException(th);
    } finally {
      db.closeStatement(sqlQuery);
    }
    return result;
  }
  
}
