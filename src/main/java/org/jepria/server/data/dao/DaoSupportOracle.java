package org.jepria.server.data.dao;

import oracle.jdbc.OracleTypes;
import org.apache.log4j.Logger;
import org.jepria.compat.server.dao.ResultSetMapper;
import org.jepria.compat.server.dao.ResultSetWrapper;
import org.jepria.compat.server.download.blob.BinaryFileDownloadImpl;
import org.jepria.compat.server.download.blob.FileDownloadStream;
import org.jepria.compat.server.download.clob.FileDownloadReader;
import org.jepria.compat.server.download.clob.TextFileDownloadImpl;
import org.jepria.compat.server.upload.blob.BinaryFileUploadImpl;
import org.jepria.compat.server.upload.blob.FileUploadStream;
import org.jepria.compat.server.upload.clob.FileUploadWriter;
import org.jepria.compat.server.upload.clob.TextFileUploadImpl;
import org.jepria.compat.shared.exceptions.ApplicationException;
import org.jepria.compat.shared.util.JepRiaUtil;
import org.jepria.server.data.RuntimeSQLException;
import org.jepria.server.data.sql.ConnectionContext;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DaoSupportOracle implements DaoSupport {
  protected static Logger logger = Logger.getLogger(DaoSupportOracle.class.getName());
  
  /**
   * Возможные типы выполнения запроса в методе {@link org.jepria.server.data.dao.DaoSupportOracle#setParamsAndExecute}.
   */
  private enum ExecutionType {
    /**
     * SQL-запрос.
     */
    QUERY,
    
    /**
     * SQL-выражение (специфичный для Oracle тип, результат исполнения &mdash; курсор).
     */
    CALLABLE_STATEMENT
  }
  
  @Override
  public <T> T create(String query, Class<? super T> resultTypeClass, Object... params) {
    T result = null;
    
    try (CallableStatement callableStatement = ConnectionContext.getInstance().prepareCall(query)) {
      setInputParamsToStatement(callableStatement, 2, params);
      
      if (resultTypeClass.equals(Integer.class)) {
        callableStatement.registerOutParameter(1, Types.INTEGER);
      } else if (resultTypeClass.equals(String.class)) {
        callableStatement.registerOutParameter(1, Types.VARCHAR);
      } else if (resultTypeClass.equals(Timestamp.class)) {
        callableStatement.registerOutParameter(1, Types.TIMESTAMP);
      } else if (resultTypeClass.equals(BigDecimal.class)) {
        callableStatement.registerOutParameter(1, Types.NUMERIC);
      } else {
        throw new ApplicationException("Unknown result type", null);
      }
      
      setApplicationInfo(query);
      // Выполнение запроса
      callableStatement.execute();
      
      result = (T) callableStatement.getObject(1);
      if (callableStatement.wasNull()) {
        result = null;
      }
      
    } catch (SQLException th) {
      throw new RuntimeSQLException((SQLException) th.getCause());
    } catch (Throwable th) {
      throw new RuntimeException(th);
    }
    
    return result;
  }
  
  @Override
  public void execute(String query, Object... params) {
    try (CallableStatement callableStatement = ConnectionContext.getInstance().prepareCall(query)) {
      setInputParamsToStatement(callableStatement, 1, params);
      
      setApplicationInfo(query);
      callableStatement.execute();
    } catch (SQLException th) {
      throw new RuntimeSQLException((SQLException) th.getCause());
    } catch (Throwable th) {
      throw new RuntimeException(th);
    }
  }
  
  @Override
  public <T> List<T> find(String query, ResultSetMapper<? super T> mapper, Class<? super T> dtoClass, Object... params) {
    try {
      return findOrSelect(query, mapper,
          dtoClass, ExecutionType.CALLABLE_STATEMENT, params);
    } catch (SQLException th) {
      throw new RuntimeSQLException((SQLException) th.getCause());
    } catch (Throwable th) {
      throw new RuntimeException(th);
    }
  }
  
  @Override
  public <T> T executeAndReturn(String query, Class<? super T> resultTypeClass, Object... params) {
    T result = null;
    
    try (CallableStatement callableStatement = ConnectionContext.getInstance().prepareCall(query)) {
      
      setInputParamsToStatement(
          callableStatement,
          resultTypeClass.isArray() ? 1 : 2,
          params);
      
      setOutputParamsToStatement(
          callableStatement,
          resultTypeClass,
          params);
      
      setApplicationInfo(query);
      callableStatement.execute();
      
      result = getResult(callableStatement, resultTypeClass, params);
    } catch (SQLException th) {
      throw new RuntimeSQLException((SQLException) th.getCause());
    } catch (Throwable th) {
      throw new RuntimeException(th);
    }
    return result;
  }
  
  @Override
  public <T> List<T> select(String query, ResultSetMapper<? super T> mapper, Class<? super T> dtoClass, Object... params) {
    try {
      return findOrSelect(query, mapper,
          dtoClass, ExecutionType.QUERY, params);
    } catch (SQLException th) {
      throw new RuntimeSQLException((SQLException) th.getCause());
    } catch (Throwable th) {
      throw new RuntimeException(th);
    }
  }
  
  @Override
  public void update(String query, Object... params) {
    execute(query, params);
  }
  
  @Override
  public void delete(String query, Object... params) {
    execute(query, params);
  }
  
  @Override
  public void deleteClob(String tableName, String dataFieldName, Map primaryKeyMap) {
    // TODO stub implementation
    
    final Reader emptyReader = new Reader() {
      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        return -1;
      }
      
      @Override
      public void close() throws IOException {
      }
    };
    
    uploadClob(tableName, dataFieldName, primaryKeyMap, emptyReader);
  }
  
  @Override
  public void uploadClob(String tableName, String dataFieldName, Map primaryKeyMap, Reader reader) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileUploadWriter.uploadFile(
          reader
          , new TextFileUploadImpl()
          , tableName
          , dataFieldName
          , primaryKeyMap // internally transformed to "where [whereClause] and 1=1"
          , null
          , null
          , false
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void deleteBlob(String tableName, String dataFieldName, Map primaryKeyMap) {
    // TODO stub implementation
    
    final InputStream emptyStream = new InputStream() {
      @Override
      public int read() throws IOException {
        return -1;
      }
    };
    
    uploadBlob(tableName, dataFieldName, primaryKeyMap, emptyStream);
  }
  
  @Override
  public void uploadBlob(String tableName, String dataFieldName, Map primaryKeyMap, InputStream stream) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileUploadStream.uploadFile(
          stream
          , new BinaryFileUploadImpl() // transaction logic is performed by org.jepria.compat.server.dao.transaction.TransactionFactory.TransactionInvocationHandler Dao wrapper
          , tableName
          , dataFieldName
          , primaryKeyMap // internally transformed to "where [whereClause] and 1=1"
          , null
          , null
          , false
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void downloadClob(String tableName, String dataFieldName, Map primaryKeyMap, Writer writer) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileDownloadReader.downloadFile(
          writer
          , new TextFileDownloadImpl()
          , tableName
          , dataFieldName
          , primaryKeyMap
          , null
          , null
          , false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void downloadBlob(String tableName, String dataFieldName, Map primaryKeyMap, OutputStream stream) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileDownloadStream.downloadFile(
          stream
          , new BinaryFileDownloadImpl()
          , tableName
          , dataFieldName
          , primaryKeyMap
          , null
          , null
          , false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void deleteClob(String tableName, String dataFieldName, String whereClause) {
    // TODO stub implementation
    
    final Reader emptyReader = new Reader() {
      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        return -1;
      }
      
      @Override
      public void close() throws IOException {
      }
    };
    
    uploadClob(tableName, dataFieldName, whereClause, emptyReader);
  }
  
  @Override
  public void uploadClob(String tableName, String dataFieldName, String whereClause, Reader reader) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileUploadWriter.uploadFile(
          reader
          , new TextFileUploadImpl()
          , tableName
          , dataFieldName
          , whereClause
          , null // internally transformed to "where [whereClause] and 1=1"
          , null
          , false
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void deleteBlob(String tableName, String dataFieldName, String whereClause) {
    // TODO stub implementation
    
    final InputStream emptyStream = new InputStream() {
      @Override
      public int read() throws IOException {
        return -1;
      }
    };
    
    uploadBlob(tableName, dataFieldName, whereClause, emptyStream);
  }
  
  @Override
  public void uploadBlob(String tableName, String dataFieldName, String whereClause, InputStream stream) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileUploadStream.uploadFile(
          stream
          , new BinaryFileUploadImpl() // transaction logic is performed by org.jepria.compat.server.dao.transaction.TransactionFactory.TransactionInvocationHandler Dao wrapper
          , tableName
          , dataFieldName
          , whereClause
          , null
          , null
          , false
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void downloadClob(String tableName, String dataFieldName, String whereClause, Writer writer) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileDownloadReader.downloadFile(
          writer
          , new TextFileDownloadImpl()
          , tableName
          , dataFieldName
          , whereClause
          , null
          , null
          , false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public void downloadBlob(String tableName, String dataFieldName, String whereClause, OutputStream stream) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileDownloadStream.downloadFile(
          stream
          , new BinaryFileDownloadImpl()
          , tableName
          , dataFieldName
          , whereClause
          , null
          , null
          , false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  /**
   * Вспомогательный метод, объединящий в себе логику работы методов find и select.
   *
   * @param <T>      тип возвращаемого значения
   * @param query    текст запроса
   * @param mapper   экземпляр класса, осуществляющего мэппинг полей dto и ResultSet
   * @param dtoClass класс записи
   * @param params   параметры sql-запроса или sql-выражения
   * @return список объектов в виде List&lt;T&gt;
   * @throws ApplicationException
   */
  private <T> List<T> findOrSelect(
      String query
      , ResultSetMapper<? super T> mapper
      , Class<? super T> dtoClass
      , ExecutionType executionType
      , Object... params)
      throws SQLException, IllegalAccessException, InstantiationException {
    
    List<T> result = new ArrayList<T>();
    
    ResultSet resultSet = null;
    
    try (CallableStatement callableStatement = ConnectionContext.getInstance().prepareCall(query)) {
      setApplicationInfo(query);
      resultSet = setParamsAndExecute(callableStatement, executionType, params);
      
      while (resultSet.next()) {
        T resultModel = (T) dtoClass.newInstance();
        
        mapper.map(resultSet, resultModel);
        
        result.add(resultModel);
      }
    } finally {
      if (resultSet != null) {
        resultSet.close();
      }
    }
    
    return result;
  }
  
  /**
   * Вспомогательный метод, выставляющий параметры callableStatement и выполняющий запрос.
   *
   * @param callableStatement экземпляр callableStatement
   * @param executionType     данная переменная используется для определения того,
   *                          работаем ли мы с sql-выражением, или sql-запросом
   * @param params            параметры sql-запроса или sql-выражения
   * @return экземпляр ResultSet
   * @throws SQLException
   */
  private ResultSet setParamsAndExecute(
      CallableStatement callableStatement, ExecutionType executionType, Object... params)
      throws SQLException {
    
    logger.trace("setParamsAndExecute(...)");
    
    ResultSet resultSet = null;
    if (executionType == ExecutionType.CALLABLE_STATEMENT) {
      callableStatement.registerOutParameter(1, OracleTypes.CURSOR);
      
      setInputParamsToStatement(callableStatement, 2, params);
      
      callableStatement.execute();
      
      //Получим набор.
      resultSet = ResultSetWrapper.wrap((ResultSet) callableStatement.getObject(1));
    } else if (executionType == ExecutionType.QUERY) {
      setInputParamsToStatement(callableStatement, 1, params);
      
      //Получим набор.
      resultSet = callableStatement.executeQuery();
    }
    return resultSet;
  }
  
  /**
   * Данный вспомогательный метод присваивает параметры запроса объекту callableStatement.
   *
   * @param callableStatement экзепляр callableStatement
   * @param i                 номер с, с которого начинаем выставлять параметры
   * @param params            параметры запроса
   * @throws SQLException
   */
  private void setInputParamsToStatement(
      CallableStatement callableStatement,
      int i,
      Object... params) throws SQLException {
    if (params.length > 0) {
      for (int index = ((params[0] != null && params[0].getClass().isArray()) ? 1 : 0); index < params.length; index++) {
        Object param = params[index];
        
        if (param != null) {
          Class<?> clazz = param.getClass();
          if (clazz.equals(Integer.class)) {
            setIntegerParameter(callableStatement, (Integer) param, i);
          } else if (clazz.equals(String.class)) {
            setStringParameter(callableStatement, (String) param, i);
          } else if (clazz.equals(Boolean.class)) {
            setBooleanParameter(callableStatement, (Boolean) param, i);
          } else if (clazz.equals(BigDecimal.class)) {
            setBigDecimalParameter(callableStatement, (BigDecimal) param, i);
          } else if (clazz.equals(java.util.Date.class)) {
            setDateParameter(callableStatement, (java.util.Date) param, i);
          } else if (clazz.equals(Clob.class)) {
            setClobParameter(callableStatement, (Clob) param, i);
          } else {
            callableStatement.setObject(i, param);
          }
        } else {
          callableStatement.setNull(i, Types.NULL);
        }
        i++;
      }
    }
  }
  
  /**
   * Служебный метод, устанавливающий выходные параметры в statement.
   *
   * @param callableStatement шаблон SQL-инструкции
   * @param resultTypeClass   тип параметра
   * @param params            массив параметров
   * @throws SQLException при возникновении ошибки JDBC
   */
  private <T> void setOutputParamsToStatement(
      CallableStatement callableStatement,
      Class<? super T> resultTypeClass,
      Object[] params) throws SQLException {
    if (resultTypeClass.isArray()) {
      Object[] outputParamTypes = (Object[]) params[0];
      for (int i = 0; i < outputParamTypes.length; i++) {
        registerParameter(callableStatement, i + params.length, (Class<? super T>) outputParamTypes[i]);
      }
    } else {
      registerParameter(callableStatement, 1, resultTypeClass);
    }
  }
  
  /**
   * Служебный метод, устанавливающий выходной параметр в statement
   *
   * @param callableStatement шаблон SQL-инструкции
   * @param paramNumber       номер параметра
   * @param resultTypeClass   тип параметра
   * @throws SQLException при возникновении ошибки JDBC
   */
  private <T> void registerParameter(
      CallableStatement callableStatement,
      int paramNumber,
      Class<? super T> resultTypeClass) throws SQLException {
    if (resultTypeClass.equals(Integer.class)) {
      callableStatement.registerOutParameter(paramNumber, Types.INTEGER);
    } else if (resultTypeClass.equals(String.class)) {
      callableStatement.registerOutParameter(paramNumber, Types.VARCHAR);
    } else if (resultTypeClass.equals(Timestamp.class)) {
      callableStatement.registerOutParameter(paramNumber, Types.TIMESTAMP);
    } else if (resultTypeClass.equals(BigDecimal.class)) {
      callableStatement.registerOutParameter(paramNumber, Types.NUMERIC);
    } else if (resultTypeClass.equals(Clob.class)) {
      callableStatement.registerOutParameter(paramNumber, Types.CLOB);
    } else {
      throw new IllegalArgumentException("Unknown result type");
    }
  }
  
  /**
   * Служебный метод, осушествляющий извлечение выходных параметров.<br/>
   * Поддерживается как извлечение единственного параметра, так и нескольких параметров.
   *
   * @param callableStatement SQL-выражение
   * @param resultTypeClass   тип возвращаемого значения или массив типов
   * @param params            параметры вызова
   * @return выходной параметр (или массив выходных параметров)
   * @throws SQLException
   */
  private <T> T getResult(
      CallableStatement callableStatement,
      Class<? super T> resultTypeClass,
      Object[] params) throws SQLException {
    T result;
    
    if (resultTypeClass.isArray()) {
      Object[] outputParamTypes = (Object[]) params[0];
      Object[] results = new Object[outputParamTypes.length];
      for (int i = 0; i < outputParamTypes.length; i++) {
        results[i] = callableStatement.getObject(i + params.length);
        if (callableStatement.wasNull()) results[i] = null;
      }
      result = (T) results;
    } else {
      result = (T) callableStatement.getObject(1);
      if (callableStatement.wasNull()) result = null;
    }
    
    return result;
  }
  
  /**
   * Установка информации о вызывающем модуле.
   * Использует встроенный функционал Oracle для установки трёх параметров сессии.
   * Значение module_name (имя модуля) извлекается из {@link ConnectionContext}, action_name
   * (название действия) - из шаблона SQL-выражения. client_info заполняется пустым значением.
   *
   * @param queryToExecute шаблон запроса
   * @throws SQLException при ошибке взаимодействия с базой
   */
  private void setApplicationInfo(String queryToExecute) throws SQLException {
    setModule(ConnectionContext.getInstance().getModuleName(), getAction(queryToExecute));
  }
  
  /**
   * Установка имени модуля (module name) и названия действия (action_name).
   * Кроме того, метод сбрасывает установленное значение client_info.
   *
   * @param moduleName имя модуля (Oracle обрезает значение до 48 байт)
   * @param actionName название действия (Oracle обрезает значение до 32 байт)
   * @throws SQLException при ошибке взаимодействия с базой
   */
  @Override
  public void setModule(String moduleName, String actionName) throws SQLException {
    /*
     * TODO: Найти способ передать в client_info полезную информацию
     * (например, id или логин вызывающего оператора).
     */
    String query =
        "begin  "
            + "dbms_application_info.set_module("
            + "module_name => ? "
            + ", action_name => ? "
            + ");"
            + "dbms_application_info.set_client_info("
            + "client_info => null "
            + ");"
            + " end;";
    Connection connection = ConnectionContext.getInstance().getConnection();
    CallableStatement callableStatement = ConnectionContext.getInstance().prepareCall(query);
    setInputParamsToStatement(callableStatement, 1, moduleName, actionName);
    callableStatement.execute();
  }
  
  /**
   * Получение названия действия (action_name) из шаблона SQL-выражения.
   * Если запрос содержит имя функции или процедуры, то метод возвращает данное имя.
   * В противном случае (например, если это SQL-запрос) в качестве названия действия
   * возвращается сам шаблон запроса.
   *
   * @param query шаблон запроса
   * @return название действия
   */
  private String getAction(String query) {
    int leftDelimiterIndex = query.indexOf('.');
    int rightDelimiterIndex = query.indexOf('(');
    if (rightDelimiterIndex == -1) {
      rightDelimiterIndex = query.indexOf(';');
    }
    if (leftDelimiterIndex != -1 && rightDelimiterIndex > leftDelimiterIndex) {
      return query.substring(leftDelimiterIndex + 1, rightDelimiterIndex);
    } else {
      return query;
    }
  }
  
  /**
   * Вспомогательный метод. Используется для задания строкового параметра объекту callableStatement.
   *
   * @param callableStatement экзепляр callableStatement
   * @param parameter         параметр
   * @param place             место вставки параметра
   * @throws SQLException
   */
  private static void setStringParameter(
      CallableStatement callableStatement
      , String parameter
      , int place)
      throws SQLException {
    
    if (JepRiaUtil.isEmpty(parameter)) {
      callableStatement.setNull(place, Types.VARCHAR);
    } else {
      callableStatement.setString(place, parameter);
    }
  }
  
  /**
   * Вспомогательный метод. Используется для задания целочисленного параметра объекту callableStatement.
   *
   * @param callableStatement экзепляр callableStatement
   * @param parameter         параметр
   * @param place             место вставки параметра
   * @throws SQLException
   */
  private static void setIntegerParameter(
      CallableStatement callableStatement
      , Integer parameter
      , int place)
      throws SQLException {
    
    if (JepRiaUtil.isEmpty(parameter)) {
      callableStatement.setNull(place, Types.INTEGER);
    } else {
      callableStatement.setInt(place, parameter);
    }
  }
  
  /**
   * Вспомогательный метод. Используется для задания булевого параметра объекту callableStatement.
   *
   * @param callableStatement экзепляр callableStatement
   * @param parameter         параметр
   * @param place             место вставки параметра
   * @throws SQLException
   */
  private static void setBooleanParameter(
      CallableStatement callableStatement
      , Boolean parameter
      , int place)
      throws SQLException {
    
    if (parameter) {
      callableStatement.setInt(place, 1);
    } else {
      callableStatement.setInt(place, 0);
    }
  }
  
  /**
   * Вспомогательный метод. Используется для задания параметра типа BigDecimal объекту callableStatement.
   *
   * @param callableStatement экзепляр callableStatement
   * @param parameter         параметр
   * @param place             место вставки параметра
   * @throws SQLException
   */
  private static void setBigDecimalParameter(
      CallableStatement callableStatement
      , BigDecimal parameter
      , int place)
      throws SQLException {
    
    if (parameter == null) {
      callableStatement.setNull(place, Types.NUMERIC);
    } else {
      callableStatement.setBigDecimal(place, parameter);
    }
  }
  
  /**
   * Вспомогательный метод. Используется для задания параметра типа Date объекту callableStatement.
   *
   * @param callableStatement экземпляр callableStatement
   * @param parameter         параметр
   * @param place             место вставки параметра
   * @throws SQLException
   */
  private static void setDateParameter(CallableStatement callableStatement, java.util.Date parameter, int place)
      throws SQLException {
    if (parameter == null) {
      callableStatement.setNull(place, Types.TIMESTAMP);
    } else {
      callableStatement.setTimestamp(place, new java.sql.Timestamp(parameter.getTime()));
    }
  }
  
  /**
   * Вспомогательный метод. Используется для задания параметра типа Clob объекту callableStatement.
   *
   * @param callableStatement экземпляр callableStatement
   * @param parameter         параметр
   * @param place             место вставки параметра
   * @throws SQLException
   */
  private static void setClobParameter(CallableStatement callableStatement, Clob parameter, int place)
      throws SQLException {
    if (JepRiaUtil.isEmpty(parameter)) {
      callableStatement.setNull(place, Types.CLOB);
    } else {
      callableStatement.setClob(place, parameter.getCharacterStream());
    }
  }
}