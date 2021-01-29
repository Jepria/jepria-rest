package org.jepria.server.transaction.annotation;

import org.jepria.server.transaction.handler.StartTransactionHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Позволяет указать кастомный обработчик завершения транзакции.
 * К примеру, кастомный обработчик может использоваться, чтобы
 * сообщить в логе о начале транзакции:
 * <pre>public class CustomDao extends JepDao implements Custom {
 *
 *  private static final Logger log = Logger.getLogger(CustomDao.class);
 *
 *  public static class CustomStartTransactionHandler implements StartTransactionHandler{
 *    {@literal @}Override
 *    public void handle(String dataSourceJndiName) throws ApplicationException {
 *      CallContext.begin(dataSourceJndiName);
 *      log.trace("Начало транзакции");
 *    }
 *  };
 *
 *  {@literal @}Override
 *  {@literal @}Before(startTransactionHandler = CustomStartTransactionHandler.class))
 *  public void transactionMethod() throws ApplicationException {    
 *    . . .  
 *  }
 *}</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Before {
  /**
   * Возвращает обработчик завершения транзакции.
   * @return обработчик завершения
   */
  Class<? extends StartTransactionHandler> startTransactionHandler();
}
