package org.jepria.compat.server.dao.transaction.handler;

import org.jepria.compat.server.dao.CallContext;
import org.jepria.compat.server.db.Db;

/**
 * Интерфейс обработчика начала транзакции.
 */
public interface StartTransactionHandler {
  /**
   * Метод, предваряющий транзакцию.<br/>
   * Требование к кастомной реализации: необходимо вызвать {@link CallContext#begin(String, String)},
   * чтобы в <code>ThreadLocal</code> был размещён объект {@link Db}.
   * @param dataSourceJndiName JNDI-имя источника данных
   * @param moduleName имя модуля для передачи в DB.
   */
  Db handle(String dataSourceJndiName, String moduleName);
}
