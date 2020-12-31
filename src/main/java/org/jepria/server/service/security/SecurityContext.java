package org.jepria.server.service.security;

import oracle.jdbc.OracleTypes;
import org.jepria.server.data.sql.ConnectionContext;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.sql.CallableStatement;
import java.sql.SQLException;

public abstract class SecurityContext implements javax.ws.rs.core.SecurityContext {

  private final String username;
  private final Integer operatorId;
  private HttpServletRequest request;

  public SecurityContext(HttpServletRequest request, String username, Integer operatorId) {
    this.request = request;
    this.username = username;
    this.operatorId = operatorId;
  }

  public boolean isRole(String datasourceJndiName, String roleShortName) throws SQLException {
    String sqlQuery =
        "begin ? := pkg_operator.isrole(" +
            "operatorid => ?, " +
            "roleshortname => ?" +
            "); " +
            "end;";
    int result;
    ConnectionContext.getInstance().begin(datasourceJndiName, "");
    try (CallableStatement callableStatement = ConnectionContext.getInstance().prepareCall(sqlQuery)) {
      callableStatement.registerOutParameter(1, OracleTypes.INTEGER);
      callableStatement.setInt(2, operatorId);
      callableStatement.setString(3, roleShortName);
      callableStatement.execute();
      result = new Integer(callableStatement.getInt(1));
      if (callableStatement.wasNull()) result = 0;
      ConnectionContext.getInstance().commit();
    } catch (SQLException exception) {
      ConnectionContext.getInstance().rollback();
      throw exception;
    } finally {
      ConnectionContext.getInstance().end();
    }
    return result == 1;
  }

  @Override
  public Principal getUserPrincipal() {
    return new PrincipalImpl(username, operatorId);
  }

  @Override
  public boolean isSecure() {
    return request.isSecure();
  }
}
