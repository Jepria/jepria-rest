package org.jepria.compat.server.dao;

import java.util.List;

import org.jepria.compat.shared.exceptions.ApplicationException;
import org.jepria.compat.shared.field.option.JepOption;
import org.jepria.compat.shared.record.JepRecord;

/**
 * Общий предок DAO стандартной работы с данными.
 */
public class JepDaoStandard {

  /**
   * Поиск записей.
   * @param sqlQuery шаблон запроса
   * @param resultSetMapper отображение содержимого <code>ResultSet</code> на {@link JepRecord}.
   * @param params параметры вызова
   * @return список записей
   * @throws ApplicationException в случае ошибки при взаимодействии с базой
   */
  public List<JepRecord> find(
      String sqlQuery,
      ResultSetMapper<JepRecord> resultSetMapper,
      Object... params) throws ApplicationException {
    return DaoSupport.find(
        sqlQuery,
        resultSetMapper,
        JepRecord.class,
        params); 
  }
  
  /**
   * Получение списка опций.
   * @param sqlQuery шаблон запроса
   * @param resultSetMapper отображение содержимого <code>ResultSet</code> на {@link JepOption} 
   * @param params параметры вызова
   * @return список опций
   * @throws ApplicationException в случае ошибки взаимодействия с базой
   */
  public List<JepOption> getOptions (
      String sqlQuery,
      ResultSetMapper<JepOption> resultSetMapper,
      Object... params) throws ApplicationException {
    return DaoSupport.find(
        sqlQuery,
        resultSetMapper,
        JepOption.class,
        params); 
  }

  /**
   * Создаёт запись и возвращает её первичный ключ.
   * @param sqlQuery шаблон запроса
   * @param resultTypeClass тип возвращаемого значения
   * @param params параметры вызова
   * @return первичный ключ
   * @throws ApplicationException в случае ошибки взаимодействия с базой
   */
  public <T> T create(
      String sqlQuery,
      Class<T> resultTypeClass,      
      Object... params) throws ApplicationException {
    return DaoSupport.<T> create(sqlQuery,
        resultTypeClass,
        params);
  }

  /**
   * Изменение записи.
   * @param sqlQuery шаблон запроса
   * @param params параметры вызова
   * @throws ApplicationException в случае ошибки взаимодействия с базой
   */
  public void update(
      String sqlQuery,
      Object... params) throws ApplicationException {
    DaoSupport.update(
        sqlQuery,
        params);
  }

  /**
   * Удаление записи.
   * @param sqlQuery шаблон запроса
   * @param params параметры вызова
   * @throws ApplicationException в случае ошибки взаимодействия с базой
   */
  public void delete(
      String sqlQuery,
      Object... params) throws ApplicationException {
    DaoSupport.delete(
        sqlQuery,
        params);
  }

}
