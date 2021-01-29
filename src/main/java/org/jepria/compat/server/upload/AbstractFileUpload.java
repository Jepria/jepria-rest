package org.jepria.compat.server.upload;

import org.jepria.compat.server.db.LargeObject;
import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.shared.exceptions.SystemException;

/**
 * Абстрактный базовый класс для загрузки файла на сервер.
 * 
 */
public abstract class AbstractFileUpload implements FileUpload {

  protected LargeObject largeObject = null;

  protected boolean cancelled = false;

  @Override
  public boolean isCancelled() {
    return cancelled;
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
    if (largeObject != null) {
      largeObject.cancel();
    }
  }
}