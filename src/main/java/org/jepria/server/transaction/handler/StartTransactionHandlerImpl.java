package org.jepria.server.transaction.handler;

import org.jepria.server.data.sql.ConnectionContext;

/**
 * Стандартная реализация обработчика старта транзакции.<br/>
 */
public class StartTransactionHandlerImpl implements StartTransactionHandler {

  /**
   * Стандартная реализация обработки начала транзакции.<br/>
   * Единственное действие &mdash; вызов {@link ConnectionContext#begin(String, String)}.
   */
  @Override
  public void handle(String dataSourceJndiName, String moduleName) {
    ConnectionContext.getInstance().begin(dataSourceJndiName, moduleName);
  }
}
