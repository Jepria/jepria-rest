package org.jepria.compat.server.upload.blob;

import org.jepria.compat.server.db.blob.BinaryLargeObject;
import org.jepria.compat.server.dao.CallContext;
import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.server.upload.AbstractFileUpload;
import org.jepria.compat.shared.exceptions.ApplicationException;

import java.util.List;

/**
 * Реализует загрузку (upload) бинарного файла.
 */
public class BinaryFileUploadImpl extends AbstractFileUpload implements BinaryFileUpload {

  /**
   * Создаёт загрузчик файлов на сервер.
   */
  public BinaryFileUploadImpl() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int beginWrite(
      String tableName
      , String fileFieldName
      , List<String> primaryKey
      , List<Object> rowId)
      throws ApplicationException {

    int result = -1;
    try {
      super.largeObject = new BinaryLargeObject(tableName, fileFieldName, primaryKey, rowId);
      result = ((BinaryLargeObject) super.largeObject).beginWrite();
    } catch (ApplicationException ex) {
      cancel();
      throw ex;
    } finally {
      storedContext = CallContext.detach();
    }

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void continueWrite(byte[] dataBlock) throws SpaceException {
    CallContext.attach(storedContext);
    boolean cancelled = false;
    try {
      ((BinaryLargeObject) super.largeObject).continueWrite(dataBlock);
    } catch (Throwable ex) {
      cancelled = true;
      if (ex instanceof SpaceException) {
        throw (SpaceException) ex;
      } else if (ex instanceof Exception) {
        throw new SpaceException("continue write error", (Exception) ex);
      } else {
        throw new SpaceException("continue write error", new RuntimeException(ex));
      }
    } finally {
      if (cancelled) {
        try {
          cancel();
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
      storedContext = CallContext.detach();
    }
  }
}
