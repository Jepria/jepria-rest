package org.jepria.compat.server.download.blob;

import org.jepria.compat.server.dao.CallContext;
import org.jepria.compat.server.db.blob.BinaryLargeObject;
import org.jepria.compat.server.download.AbstractFileDownload;
import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.shared.exceptions.ApplicationException;

import java.util.Map;

/**
 * Класс, реализующий выгрузку (download) бинарного файла.
 */
public class BinaryFileDownloadImpl extends AbstractFileDownload implements BinaryFileDownload {

  /**
   * Метод начинает чтение данных из LOB.
   *
   * @return рекомендуемая величина буфера
   * @throws ApplicationException
   */
  @Override
  public int beginRead(
      String tableName
      , String fileFieldName
      , Map primaryKeyMap
      )
      throws ApplicationException {

    int result = -1;
    try {

      super.largeObject = new BinaryLargeObject(tableName, fileFieldName, primaryKeyMap);
      result = ((BinaryLargeObject)super.largeObject).beginRead();
    } catch (Throwable th) {
      cancel();
      throw th;
    } finally {
      storedContext = CallContext.detach();
    }

    return result;
  }

  /**
   * Метод начинает чтение данных из LOB.
   *
   * @return рекомендуемая величина буфера
   * @throws ApplicationException
   */
  @Override
  public int beginRead(
      String tableName
      , String fileFieldName
      , String whereClause
  )
      throws ApplicationException {

    int result = -1;
    try {
      super.largeObject = new BinaryLargeObject(tableName, fileFieldName, whereClause);
      result = ((BinaryLargeObject)super.largeObject).beginRead();
    } catch (Throwable th) {
      cancel();
      throw th;
    } finally {
      storedContext = CallContext.detach();
    }

    return result;
  }

  /**
   * Чтение очередного блока данных из BINARY_FILE.
   *
   * @param dataBlock блок данных
   * @throws SpaceException
   */
  @Override
  public int continueRead(byte[] dataBlock) throws SpaceException {
    CallContext.attach(storedContext);
    boolean cancelled = true;
    int result = 0;
    try {
      result = ((BinaryLargeObject)super.largeObject).continueRead(dataBlock);
      cancelled = false;
    } catch (SpaceException e) {
      throw e;
    } catch (Exception e) {
      throw new SpaceException("continue read error", (Exception) e);
    } finally {
      if (cancelled) {
        cancel();
      }
      storedContext = CallContext.detach();
    }

    return result;
  }
}
