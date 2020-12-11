package org.jepria.server.transaction.handler;

import java.sql.Connection;

/**
 * Интерфейс обработчика начала транзакции.
 */
public interface StartTransactionHandler {
  /**
   * Метод, предваряющий транзакцию.<br/>
   * Требование к кастомной реализации: необходимо вызвать {@link org.jepria.server.data.sql.ConnectionContext#begin(String, String)},
   * чтобы в <code>ThreadLocal</code> был размещён объект {@link Connection}.
   * @param dataSourceJndiName JNDI-имя источника данных
   * @param moduleName имя модуля для передачи в DB.
   */
  void handle(String dataSourceJndiName, String moduleName);
}
