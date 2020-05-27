package org.jepria.compat.server.upload;

import org.jepria.compat.server.dao.CallContext;
import org.jepria.compat.server.db.LargeObject;
import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.shared.exceptions.ApplicationException;
import org.jepria.compat.shared.exceptions.SystemException;

/**
 * Абстрактный базовый класс для загрузки файла на сервер.
 * 
 */
public abstract class AbstractFileUpload implements FileUpload {

  protected CallContext storedContext;
  protected LargeObject largeObject = null;

  protected boolean cancelled = false;

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * Функция-обертка для {@link #beginWrite(String tableName, String fileFieldName, String keyFieldName, Object rowId)}.
   * В классе реализации в конкретном модуле данный метод перегружаем вызывая в нем 
   * {@link #beginWrite(String tableName, String fileFieldName, String keyFieldName, Object rowId)}
   * с подставленными из констант класса реализации параметрами:<br/>
   * <code>
   * tableName,<br/>
   * fileFieldName,<br/>
   * keyFieldName,<br/>
   * dataSourceJndiName<br/>
   * </code>.
   * В данном базовом классе содержит пустую реализацию, возвращающую 0.
   * 
   * @param rowId         идентификатор строки таблицы
   * @return рекомендуемый размер буфера
   * @throws ApplicationException 
   */
  @Override
  public int beginWrite(Object rowId) 
    throws ApplicationException {

    return 0;
  }  

  /**
   * Окончание выгрузки.
   * После выполнения этого метода stateful bean должен быть удалён. 
   * Для удаления bean необходимо в классе реализации перед методом указать декларацию Remove.
   *
   * @throws SpaceException
   */
  @Override
  public void endWrite() throws SpaceException {
    CallContext.attach(storedContext);
    try {
      largeObject.endWrite();
    } catch (SpaceException ex) {
      cancel();
      throw ex;
    } catch (Throwable th) {
      th.printStackTrace();
      throw new SystemException("end write error", new RuntimeException(th));
    }
  }

  /**
   * Откат длинной транзакции.
   * После выполнения этого метода stateful bean должен быть удалён. 
   * Для удаления bean необходимо в классе реализации перед методом указать декларацию Remove.
   */
  @Override
  public void cancel() {
    cancelled = true;
    if (storedContext != null) {
      CallContext.attach(storedContext);
    }
    if (largeObject != null) {
      largeObject.cancel();
    }
  }
}