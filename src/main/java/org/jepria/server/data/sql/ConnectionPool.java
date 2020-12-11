package org.jepria.server.data.sql;

import java.sql.Connection;

public interface ConnectionPool {
  Connection createConnection(String datasourceJndiName);
  Connection createConnection(String datasourceJndiName, boolean autoCommit);
}
