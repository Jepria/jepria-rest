package org.jepria.compat.server.download;

import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.shared.exceptions.ApplicationException;

import java.util.List;
import java.util.Map;

/**
 * Интерфейс загрузки файла.
 */
public interface FileDownload {
  /**
   * Начало длинной транзакции чтения файла из LOB.
   *
   * @param tableName     имя таблицы, из которой выполняется чтение
   * @param primaryKeyMap   имя поля, из которого выполняется чтение
   * @return рекомендуемый размер буфера
   * @throws ApplicationException
   */
  int beginRead(
      String tableName
      , String fileFieldName
      , Map primaryKeyMap
  )
      throws ApplicationException;
  /**
   * Начало длинной транзакции чтения файла из LOB.
   *
   * @param tableName     имя таблицы, из которой выполняется чтение
   * @param whereClause   SQL условие
   * @return рекомендуемый размер буфера
   * @throws ApplicationException
   */
  int beginRead(
      String tableName
      , String fileFieldName
      , String whereClause
  )
      throws ApplicationException;
  
  /**
   * Метод начинает чтение данных из LOB. 
   * 
   * @param rowId идентификатор строки таблицы
   * @return рекомендуемая величина буфера
   * @throws ApplicationException
   */
  int beginRead(
      Object rowId) 
      throws ApplicationException;
  
  /**
   * Окончание загрузки.
   * После выполнения этого метода stateful bean должен быть удалён. 
   * Для удаления bean необходимо в классе реализации перед методом указать декларацию Remove.
   *
   * @throws SpaceException
   */
  void endRead() throws SpaceException;
  
  /**
   * Откат длинной транзакции.
   * После выполнения этого метода stateful bean должен быть удалён. 
   * Для удаления bean необходимо в классе реализации перед методом указать декларацию Remove.
   */
  void cancel();

  /**
   * Whether the reading has been cancelled (interrupted) or ended successfully (to decide whether to do rollback or commit at the end)
   */
  boolean isCancelled();
}
