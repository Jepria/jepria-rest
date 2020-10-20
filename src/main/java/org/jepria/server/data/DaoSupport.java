package org.jepria.server.data;

import org.jepria.compat.server.dao.ResultSetMapper;

import java.io.*;
import java.util.List;

public interface DaoSupport {

  // Зачем фабричный метод? Поскольку в перспективе может понадобиться, например,
  // кешировать инстанс, реализовать его одиночкой или давать ту или иную реализацию в зависимости от найденного DB-драйвера или других параметров.
  // Поэтому лучше изначально не давать повода писать в прикладном коде 'new DaoSupportOracle()' (хотя такую возможность отменять тоже не стоит).
  static DaoSupport getInstance() {
    return new DaoSupportOracle();
  }

  /**
   *
   * @param query
   * @param resultTypeClass
   * @param params
   * @param <T>
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   * @return
   */
  <T> T create(String query, Class<? super T> resultTypeClass, Object... params);

  /**
   *
   * @param query
   * @param params
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  void execute(String query, Object... params);

  /**
   *
   * @param query
   * @param mapper
   * @param recordClass
   * @param params
   * @param <T>
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   * @return
   */
  <T> List<T> find(String query, ResultSetMapper<? super T> mapper, Class<? super T> recordClass, Object... params);

  /**
   *
   * @param query
   * @param resultTypeClass
   * @param params
   * @param <T>
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   * @return
   */
  <T> T executeAndReturn(String query, Class<? super T> resultTypeClass, Object... params);

  /**
   *
   * @param query
   * @param mapper
   * @param modelClass
   * @param params
   * @param <T>
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   * @return
   */
  <T> List<T> select(String query, ResultSetMapper<? super T> mapper, Class<? super T> modelClass, Object... params);

  /**
   *
   * @param query
   * @param params
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  void update(String query, Object... params);

  /**
   *
   * @param query
   * @param params
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  void delete(String query, Object... params);

  /**
   *
   * @param tableName
   * @param dataFieldName
   * @param primaryKey
   * @param primaryKeyValues
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  void deleteClob(String tableName, String dataFieldName, List<String> primaryKey, List<Object> primaryKeyValues);

  /**
   * @param tableName
   * @param dataFieldName field in the table which to upload object data to
   * @param primaryKey
   * @param primaryKeyValues
   * @param reader object data source. Passing {@code null} to erase the existing content is discouraged (may cause exceptions), use {@link #deleteClob}
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  void uploadClob(String tableName, String dataFieldName, List<String> primaryKey, List<Object> primaryKeyValues, Reader reader);

  /**
   *
   * @param tableName
   * @param dataFieldName
   * @param primaryKey
   * @param primaryKeyValues
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  void deleteBlob(String tableName, String dataFieldName, List<String> primaryKey, List<Object> primaryKeyValues);

  /**
   *
   * @param tableName
   * @param dataFieldName field in the table which to upload object data to
   * @param primaryKey
   * @param primaryKeyValues
   * @param stream object data source. Passing {@code null} to erase the existing content is discouraged (may cause exceptions), use {@link #deleteBlob}
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  void uploadBlob(String tableName, String dataFieldName, List<String> primaryKey, List<Object> primaryKeyValues, InputStream stream);

  /**
   * @param tableName
   * @param dataFieldName
   * @param primaryKey
   * @param primaryKeyValues
   * @param content object data. Passing {@code null} (as well as passing {@code ""}) erases the existing content
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  default void uploadClob(String tableName, String dataFieldName, List<String> primaryKey, List<Object> primaryKeyValues, String content) {
    if (content == null || "".equals(content)) {
      deleteClob(tableName, dataFieldName, primaryKey, primaryKeyValues);
    } else {
      uploadClob(tableName, dataFieldName, primaryKey, primaryKeyValues, new StringReader(content));
    }
  }

  /**
   *
   * @param tableName
   * @param dataFieldName
   * @param primaryKey
   * @param primaryKeyValues
   * @param stream object data source. Passing {@code null} to erase the existing content is discouraged (may cause exceptions), use {@link #deleteClob}
   * @param charset
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  default void uploadClob(String tableName, String dataFieldName, List<String> primaryKey, List<Object> primaryKeyValues, InputStream stream, String charset) {
    final Reader reader;
    try {
      reader = new InputStreamReader(stream, charset);
    } catch (UnsupportedEncodingException e) {
      // TODO better to declare throws or wrap into a RuntimeException?
      throw new RuntimeException(e);
    }
    uploadClob(tableName, dataFieldName, primaryKey, primaryKeyValues, reader);
  }

  /**
   * @param tableName
   * @param dataFieldName field in the table which to upload object data to
   * @param primaryKey
   * @param primaryKeyValues
   * @param writer object data target. If the object is {@code null} (as well as empty) in the database, no data will be written
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  // no charset parameter because the database already knows which charset it uses to store clob
  void downloadClob(String tableName, String dataFieldName, List<String> primaryKey, List<Object> primaryKeyValues, Writer writer);

  /**
   *
   * @param tableName
   * @param dataFieldName field in the table which to upload object data to
   * @param primaryKey
   * @param primaryKeyValues
   * @param stream object data target
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   */
  void downloadBlob(String tableName, String dataFieldName, List<String> primaryKey, List<Object> primaryKeyValues, OutputStream stream);

  /**
   *
   * @param tableName
   * @param dataFieldName
   * @param primaryKey
   * @param primaryKeyValues
   * @throws org.jepria.server.data.RuntimeSQLException в случае {@link java.sql.SQLException}
   * @throws java.lang.RuntimeException в случае любого другого исключения
   * @return either non-empty string containing object data or {@code null} if the object is {@code null} (as well as empty) in the database
   */
  // no charset parameter because the database already knows which charset it uses to store clob
  default String downloadClob(String tableName, String dataFieldName, List<String> primaryKey, List<Object> primaryKeyValues) {
    StringWriter stringWriter = new StringWriter();
    downloadClob(tableName, dataFieldName, primaryKey, primaryKeyValues, stringWriter);
    // TODO return null if there is no clob (empty clob), instead of a clob with value ""
    return stringWriter.toString();
  }
}
