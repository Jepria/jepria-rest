package org.jepria.server.data.sql;

import org.apache.log4j.Logger;
import org.jepria.compat.server.dao.OracleCallableStatementWrapper;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Класс, реализующий получение соединения, управление транзакцией и освобождение ресурсов.<br/>
 * Объект соединения хранится в ходе выполнения в {@code ThreadLocal}.
 */
public class CallContext {
  
  private static class ConnectionStackItem {
    private final Connection connection;
    private final String dataSourceJndiName;
    private final String moduleName;
    
    public ConnectionStackItem(Connection connection, String dataSourceJndiName, String moduleName) {
      this.connection = connection;
      this.dataSourceJndiName = dataSourceJndiName;
      this.moduleName = moduleName;
    }
  
    public Connection getConnection() {
      return connection;
    }
  
    public String getDataSourceJndiName() {
      return dataSourceJndiName;
    }
  
    public String getModuleName() {
      return moduleName;
    }
  }
  
  protected static Logger logger = Logger.getLogger(CallContext.class.getName());
  
  private CallContext() {}
  
  private static final ThreadLocal<CallContext> threadLocal = new ThreadLocal<>();
  
  /**
   * Returns the thread local singleton instance
   */
  public static CallContext getInstance() {
    CallContext connectionContext = threadLocal.get();
    if (connectionContext == null) {
      connectionContext = new CallContext();
      threadLocal.set(connectionContext);
    }
    logger.debug(CallContext.class + ".getInstance():" + connectionContext);
    return connectionContext;
  }
  
  private List<CallableStatement> statements = new LinkedList<>();
  private Stack<ConnectionStackItem> connections = new Stack<>();
  private ConnectionPool pool = ConnectionPoolImpl.INSTANCE;
  
  /**
   * Метод, предваряющий начало транзакции.<br/>
   * Создаёт новый экземпляр контекста и помещает в {@code ThreadLocal}.
   * @param dataSourceJndiName JNDI-имя источника данных
   */
  public void begin(String dataSourceJndiName, String moduleName) {
    logger.trace(CallContext.class + ".beginTransaction(" + dataSourceJndiName + "," + moduleName + ")");
    createConnection(dataSourceJndiName, moduleName);
  }
  
  public void createConnection(String dataSourceJndiName, String moduleName) {
    Connection connection = pool.createConnection(dataSourceJndiName);
    logger.trace(Connection.class + ": new Connection(" + connection + ") for " + threadLocal.get());
    ConnectionStackItem item = new ConnectionStackItem(connection, dataSourceJndiName, moduleName);
    connections.add(item);
  }
  
  /**
   * Возвращает объект соединения.
   * @return объект соединения
   */
  public Connection getConnection() {
    return connections.peek().getConnection();
  }
  
  /**
   * Создает statement от актуального соединения
   * @param sql
   * @return
   */
  public CallableStatement prepareCall(String sql) throws SQLException {
    CallableStatement cs = OracleCallableStatementWrapper.wrap(getConnection().prepareCall(sql));
    // Закэшируем statement, чтобы закрыть его при окончании работы с базой, если они не были закрыты ранее. Tomcat не закрывает statement'ы автоматически!
    statements.add(cs);
    return cs;
  }
  
  /**
   * Возвращает имя модуля для передачи в DB
   * @return имя модуля
   */
  public String getModuleName() {
    return connections.peek().getModuleName();
  }
  
  /**
   * Возвращает имя модуля для передачи в DB
   * @return имя модуля
   */
  public String getDataSourceJndiName() {
    return connections.peek().getDataSourceJndiName();
  }

  /**
   * Метод, освобождающий ресурсы после завершения работы.<br/>
   * Отвечает за закрытие соединений и других ресурсов, а также
   * удаление контекста из {@code ThreadLocal}
   */
  public void end() {
    CallContext result = threadLocal.get();
    threadLocal.remove();
    if (result == null) {
      return;
    }
    result.statements.forEach(cs -> {
      try {
        if (!cs.isClosed()) {
          cs.close();
        }
      } catch (SQLException sqlException) {
        sqlException.printStackTrace();
      }
    });
    result.connections.forEach(c -> {
      try {
        if (!c.getConnection().isClosed()) {
          c.getConnection().close();
        }
      } catch (SQLException sqlException) {
        sqlException.printStackTrace();
      }
    });
  }
  
  /**
   * Выполняет закрытие (close) текущего соединения.
   * @throws SQLException в случае, если соединение выбросило исключение
   */
  public void close() {
    try {
      connections.pop().getConnection().close();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
  }
  
  /**
   * Выполняет фиксацию (commit) текущей транзакции.
   * @throws SQLException в случае, если соединение выбросило исключение
   */
  public void commit() {
    try {
      getConnection().commit();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
  }
  
  /**
   * Выполняет откат (rollback) текущей транзакции.
   * @throws SQLException в случае, если соединение выбросило исключение
   */
  public void rollback() {
    try {
      getConnection().rollback();
    } catch (SQLException exception) {
      exception.printStackTrace();
    }
  }
}
