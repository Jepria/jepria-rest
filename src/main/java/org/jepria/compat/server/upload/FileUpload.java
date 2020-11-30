package org.jepria.compat.server.upload;

import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.shared.exceptions.ApplicationException;

import java.util.List;
import java.util.Map;

/**
 * Интерфейс выгрузки файла
 */
public interface FileUpload {

  /**
   * Начало длинной транзакции записи файла в LOB.
   *
   * @param tableName     имя таблицы, в которую выполняется запись
   * @param fileFieldName   имя поля, в которое выполняется запись
   * @param primaryKeyMap   имя поля, идентифицирующего строку таблицы
   * @return рекомендуемый размер буфера
   * @throws ApplicationException
   */
  int beginWrite(
      String tableName
      , String fileFieldName
      , Map primaryKeyMap)
      throws ApplicationException;

  /**
   * Начало длинной транзакции записи файла в LOB.
   *
   * @param tableName     имя таблицы, в которую выполняется запись
   * @param fileFieldName   имя поля, в которое выполняется запись
   * @param whereClause   SQL условие
   * @return рекомендуемый размер буфера
   * @throws ApplicationException
   */
  int beginWrite(
      String tableName
      , String fileFieldName
      , String whereClause)
      throws ApplicationException;

  /**
   * Функция-обертка для {@link #beginWrite(String tableName, String fileFieldName, Map primaryKeyMap)}.
   * В классе реализации в конкретном модуле данный метод перегружаем вызывая в нем 
   * {@link #beginWrite(String, String, Map)}
   * с подставленными из констант класса реализации параметрами:<br/>
   * <code>
   * tableName,<br/>
   * fileFieldName,<br/>
   * keyFieldName,<br/>
   * dataSourceJndiName<br/>
   * </code>.
   * 
   * @param rowId         идентификатор строки таблицы
   * @return рекомендуемый размер буфера
   * @throws ApplicationException 
   */
  int beginWrite(
    Object rowId) 
    throws ApplicationException;

  /**
   * Окончание выгрузки.
   *
   * @throws SpaceException
   */
  void endWrite() throws SpaceException;

  /**
   * Откат длинной транзакции.
   */
  void cancel();

  /**
   * Whether the writing has been cancelled (interrupted) or ended successfully (to decide whether to do rollback or commit at the end)
   */
  boolean isCancelled();
}
