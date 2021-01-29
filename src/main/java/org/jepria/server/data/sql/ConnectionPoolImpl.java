package org.jepria.server.data.sql;

import org.apache.log4j.Logger;
import org.jepria.compat.shared.exceptions.SystemException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ConnectionPoolImpl implements ConnectionPool {
  INSTANCE {
    private Logger logger = Logger.getLogger(ConnectionPoolImpl.class.getName());
    private Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();
  
    @Override
    public Connection createConnection(String dataSourceJndiName) {
      return createConnection(dataSourceJndiName, false);
    }
  
    @Override
    public Connection createConnection(String dataSourceJndiName, boolean autoCommit) {
      logger.trace("BEGIN createConnection(" + dataSourceJndiName + ")");
    
      try {
        DataSource dataSource = dataSourceMap.get(dataSourceJndiName);
        if (dataSource == null) {
          InitialContext ic = new InitialContext();
          try {
            dataSource = (DataSource) ic.lookup(dataSourceJndiName);  // Для oc4j и weblogic
          } catch (NamingException nex) { // Теперь пробуем в другом контексте (для Tomcat)
            logger.trace("Failed lookup for '" + dataSourceJndiName + "', try now '" + "java:/comp/env/" + dataSourceJndiName + "'");
            dataSourceJndiName = "java:/comp/env/" + dataSourceJndiName;
            dataSource = (DataSource) ic.lookup(dataSourceJndiName);
          }
          logger.trace("Successfull lookup for " + dataSourceJndiName);
        
          dataSourceMap.put(dataSourceJndiName, dataSource);
        }
        Connection con = dataSource.getConnection();
        con.setAutoCommit(autoCommit);
        return con;
      } catch (NamingException ex) {
        logger.error(ex);
        throw new SystemException("DataSource '" + dataSourceJndiName + "' not found", ex);
      } catch (SQLException ex) {
        logger.error(ex);
        throw new SystemException("Connection creation error for '" + dataSourceJndiName + "' dataSource", ex);
      } finally {
        logger.trace("END createConnection(" + dataSourceJndiName + ")");
      }
    }
  }
}
