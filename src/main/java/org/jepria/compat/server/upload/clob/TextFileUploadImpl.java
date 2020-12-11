package org.jepria.compat.server.upload.clob;

import org.jepria.compat.server.db.clob.TextLargeObject;
import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.server.upload.AbstractFileUpload;
import org.jepria.compat.shared.exceptions.ApplicationException;

import java.util.Map;

/**
 * Класс, реализующий загрузку (upload) файла в CLOB.
 */
public class TextFileUploadImpl extends AbstractFileUpload implements TextFileUpload {

  /**
   * Создаёт загрузчик файлов на сервер.
   */
  public TextFileUploadImpl() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int beginWrite(
      String tableName
      , String fileFieldName
      , Map primaryKeyMap)
      throws ApplicationException {

    int result = -1;
    try {
      super.largeObject = new TextLargeObject(tableName, fileFieldName, primaryKeyMap);
      result = ((TextLargeObject) super.largeObject).beginWrite();
    } catch (ApplicationException ex) {
      cancel();
      throw ex;
    }

    return result;
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public int beginWrite(
      String tableName
      , String fileFieldName
      , String whereClause)
      throws ApplicationException {

    int result = -1;
    try {
      super.largeObject = new TextLargeObject(tableName, fileFieldName, whereClause);
      result = ((TextLargeObject) super.largeObject).beginWrite();
    } catch (ApplicationException ex) {
      cancel();
      throw ex;
    }

    return result;
  }
  
  @Override
  public int beginWrite(Object rowId) {
    throw new UnsupportedOperationException();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void continueWrite(char[] dataBlock) throws SpaceException {
    boolean cancelled = false;
    try {
      ((TextLargeObject) super.largeObject).continueWrite(dataBlock);
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
    }
  }
}