package org.jepria.compat.server.db;

import org.jepria.compat.server.exceptions.SpaceException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Абстрактный класс поддерживает запись в поле LOB.
 */
public abstract class LargeObject {
  protected String sqlClearLob;
  protected String sqlObtainOutputStream;
  protected String sqlObtainInputStream;
  protected Db database;
  
  // Параметры, идентифицирующие изменяемое поле Blob
  protected String tableName;
  protected List<String> primaryKey;
  

  /**
   * Освобождение ресурсов
   */
  protected abstract void closeAll();

  /**
   * Завершение записи
   */
  public abstract void endWrite() throws SpaceException;
  
  /**
   * Завершение чтения
   */
  public abstract void endRead() throws SpaceException;

  /**
   * Конструктор
   * 
   * @param tableName имя таблицы, в которую выполняется запись
   * @param fileFieldName имя поля, в которую выполняется запись
   * @param primaryKey имя поля, идентифицирующего строку таблицы
   * @param rowIds идентификатор строки таблицы
   */
  public LargeObject(String tableName, String fileFieldName, List<String> primaryKey, List<Object> rowIds) {
    checkParameters(tableName, fileFieldName, primaryKey);
    this.tableName = tableName;
    this.primaryKey = primaryKey;

    rowIds = rowIds.stream().map(rowId -> String.class.isInstance(rowId) ? "'" + rowId + "'" : rowId).collect(Collectors.toList());

    String queryString = buildSqlString(primaryKey, rowIds);

    this.sqlObtainOutputStream = "select " + fileFieldName + " from " + tableName + " where " + queryString + " for update";
    this.sqlObtainInputStream = "select " + fileFieldName + " from " + tableName + " where " + queryString;
  }

  /**
   * Отмена записи.
   */
  public void cancel() {
    closeAll();
  }

  protected String buildSqlString(List<String> primaryKey, List<Object> rowIds) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < primaryKey.size(); i++) {
      stringBuilder.append(primaryKey.get(i));
      stringBuilder.append("=");
      stringBuilder.append(rowIds.get(i));
      if (i != primaryKey.size() - 1) {
        stringBuilder.append(" and ");
      }
    }
    return stringBuilder.toString();
  }

  /**
   * Функция проверки наличия обязательных параметров конструктора. При значении null любого параметра выбрасывается исключение.
   *
   * @param tableName имя таблицы, в которую выполняется запись
   * @param fileFieldName имя поля, в которую выполняется запись
   * @param primaryKey имя поля, идентифицирующего строку таблицы
   */
  private void checkParameters(String tableName, String fileFieldName, List<String> primaryKey) {
    if (tableName == null) {
      throw new NullPointerException("Table name should be specified");
    }
    if (fileFieldName == null) {
      throw new NullPointerException("Lob field name should be specified");
    }
    if (primaryKey == null) {
      throw new NullPointerException("Key field name should be specified");
    }
  }
}