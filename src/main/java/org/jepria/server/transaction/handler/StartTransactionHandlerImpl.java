package org.jepria.server.transaction.handler;

import org.jepria.server.data.sql.CallContext;

/**
 * Стандартная реализация обработчика старта транзакции.<br/>
 */
public class StartTransactionHandlerImpl implements StartTransactionHandler {

  /**
   * Стандартная реализация обработки начала транзакции.<br/>
   * Единственное действие &mdash; вызов {@link CallContext#begin(String, String)}.
   */
  @Override
  public void handle(String dataSourceJndiName, String moduleName) {
    CallContext.getInstance().begin(dataSourceJndiName, moduleName);
  }
}
