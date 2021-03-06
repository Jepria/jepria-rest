package org.jepria.server;

import org.jepria.compat.server.dao.transaction.TransactionFactory;

/**
 * Серверная фабрика.
 * @param <D> интерфейс Dao
 */
public class ServerFactory<D> {
  
  /**
   * Исходный объект Dao.
   */
  private final D dao;
  /**
   * Прокси для Dao, обеспечивающий транзакционность.
   */
  private D proxyDao;
  /**
   * JNDI-имя источника данных.
   */
  private String dataSourceJndiName;
  /**
   * Имя модуля (module_name), передаваемое в DB.
   */
  private String moduleName;

  /**
   * Создаёт серверную фабрику.
   * @param dao объект Dao
   * @param dataSourceJndiName JNDI-имя источника данных
   */
  public ServerFactory(D dao, String dataSourceJndiName){
    this.dao = dao;
    this.dataSourceJndiName = dataSourceJndiName;
  }

  /**
   * Возвращает прокси для Dao, обеспечивающий транзакционность.
   */
  public D getDao() {
    if (proxyDao == null) {
      proxyDao = TransactionFactory.createProxy(dao, dataSourceJndiName, moduleName);
    }
    return proxyDao;
  }

  /**
   * Возвращает JNDI-имя источника данных.
   * @return JNDI-имя источника данных.
   */
  public String getDataSourceJndiName() {
    return dataSourceJndiName;
  }

  /**
   * Возвращает имя модуля для передачи в DB.
   * @return имя модуля
   */
  public String getModuleName() {
    return moduleName;
  }

  /**
   * Устанавливает имя модуля для передачи в DB.
   * @param moduleName имя модуля.
   */
  public void setModuleName(String moduleName) {
    this.moduleName = moduleName;
  }
}
