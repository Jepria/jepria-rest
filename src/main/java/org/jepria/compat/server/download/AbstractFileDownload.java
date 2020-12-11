package org.jepria.compat.server.download;

import org.jepria.compat.server.db.LargeObject;
import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.shared.exceptions.ApplicationException;
import org.jepria.compat.shared.exceptions.SystemException;

/**
 * Абстрактный базовый класс для реализаций выгрузки файла.
 */
public abstract class AbstractFileDownload implements FileDownload {

  protected LargeObject largeObject = null;

  protected boolean cancelled = false;

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * Метод начинает чтение данных из LOB. 
   * 
   * @param rowId идентификатор строки таблицы
   * @return рекомендуемая величина буфера
   * @throws ApplicationException
   */
  @Override
  public abstract int beginRead(Object rowId) throws ApplicationException;
  
  /**
   * Метод завершает чтение данных из LOB.
   * 
   * @throws SpaceException
   */
  @Override
  public void endRead() throws SpaceException {
    try {
      largeObject.endRead();
    } catch (SpaceException ex) {
      cancel();
      throw ex;
    } catch (Throwable th) {
      th.printStackTrace();
      throw new SystemException("end write error", new RuntimeException(th));
    }
  }
  
  /**
   * Метод отменяет текущую операцию и откатывает транзакцию.
   */
  @Override
  public void cancel() {
    cancelled = true;
    if (largeObject != null) {
      largeObject.cancel();
    }
  }

}
