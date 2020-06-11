package org.jepria.compat.server.upload.clob;

import org.jepria.compat.server.dao.CallContext;
import org.jepria.compat.server.db.clob.TextLargeObject;
import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.server.upload.AbstractFileUpload;
import org.jepria.compat.shared.exceptions.ApplicationException;

/**
 * Класс, реализующий загрузку (upload) файла в CLOB.
 */
public class TextFileUploadImpl extends AbstractFileUpload implements TextFileUpload {

  /**
   * Создаёт загрузчик файлов на сервер.
   */
  public TextFileUploadImpl(){
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int beginWrite(
    String tableName
    , String fileFieldName
    , String keyFieldName
    , Object rowId)
    throws ApplicationException {

    int result = -1;
    try {
      super.largeObject = new TextLargeObject(tableName, fileFieldName, keyFieldName, rowId);
      result = ((TextLargeObject)super.largeObject).beginWrite();
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
  public void continueWrite(char[] dataBlock) throws SpaceException {
    CallContext.attach(storedContext);
    boolean cancelled = false;
    try {
      ((TextLargeObject)super.largeObject).continueWrite(dataBlock);
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