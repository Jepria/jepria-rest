package org.jepria.compat.server.db;

import org.jepria.compat.server.exceptions.SpaceException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Абстрактный класс поддерживает запись в поле LOB.
 */
public abstract class LargeObject {
  protected String whereClause;
  protected String sqlClearLob;
  protected String sqlObtainOutputStream;
  protected String sqlObtainInputStream;
  
  // Параметры, идентифицирующие изменяемое поле Blob
  protected String tableName;
  protected Map primaryKeyMap;
  

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
   * @param primaryKeyMap имя поля, идентифицирующего строку таблицы
   */
  public LargeObject(String tableName, String fileFieldName, Map primaryKeyMap) {
    checkParameters(tableName, fileFieldName, primaryKeyMap);
    this.tableName = tableName;
    this.primaryKeyMap = primaryKeyMap;

    String queryString = buildSqlString(primaryKeyMap);

    this.sqlObtainOutputStream = "select " + fileFieldName + " from " + tableName + " where " + queryString + " for update";
    this.sqlObtainInputStream = "select " + fileFieldName + " from " + tableName + " where " + queryString;
  }
  /**
   * Конструктор
   *
   * @param tableName имя таблицы, в которую выполняется запись
   * @param fileFieldName имя поля, в которую выполняется запись
   * @param whereClause SQL условие
   */
  public LargeObject(String tableName, String fileFieldName, String whereClause) {
    checkParameters(tableName, fileFieldName, whereClause);
    this.tableName = tableName;
    this.whereClause = whereClause;

    this.sqlObtainOutputStream = "select " + fileFieldName + " from " + tableName + " where " + whereClause + " for update";
    this.sqlObtainInputStream = "select " + fileFieldName + " from " + tableName + " where " + whereClause;
  }

  /**
   * Отмена записи.
   */
  public void cancel() {
    closeAll();
  }

  protected String buildSqlString(Map primaryKeyMap) {
    StringBuilder stringBuilder = new StringBuilder();
    int i = 0;
    for(Object entry: primaryKeyMap.entrySet()) {
      stringBuilder.append(((Map.Entry) entry).getKey());
      stringBuilder.append("=");
      stringBuilder.append(String.class.isInstance(((Map.Entry) entry).getValue()) ? "'" + ((Map.Entry) entry).getValue() + "'" : ((Map.Entry) entry).getValue());
      if (i != primaryKeyMap.size() - 1) {
        stringBuilder.append(" and ");
      }
      i+=1;
    }
    return stringBuilder.toString();
  }

  /**
   * Функция проверки наличия обязательных параметров конструктора. При значении null любого параметра выбрасывается исключение.
   *
   * @param tableName имя таблицы, в которую выполняется запись
   * @param fileFieldName имя поля, в которую выполняется запись
   * @param primaryKeyMap имя поля, идентифицирующего строку таблицы
   */
  private void checkParameters(String tableName, String fileFieldName, Map primaryKeyMap) {
    if (tableName == null) {
      throw new NullPointerException("Table name should be specified");
    }
    if (fileFieldName == null) {
      throw new NullPointerException("Lob field name should be specified");
    }
    if (primaryKeyMap == null) {
      throw new NullPointerException("Key field name should be specified");
    }
  }

  /**
   * Функция проверки наличия обязательных параметров конструктора. При значении null любого параметра выбрасывается исключение.
   *
   * @param tableName имя таблицы, в которую выполняется запись
   * @param fileFieldName имя поля, в которую выполняется запись
   * @param whereClause SQL условие
   */
  private void checkParameters(String tableName, String fileFieldName, String whereClause) {
    if (tableName == null) {
      throw new NullPointerException("Table name should be specified");
    }
    if (fileFieldName == null) {
      throw new NullPointerException("Lob field name should be specified");
    }
    if (whereClause == null) {
      throw new NullPointerException("Key field name should be specified");
    }
  }
}