package org.jepria.compat.server.download.clob;

import org.jepria.compat.server.db.clob.TextLargeObject;
import org.jepria.compat.server.download.AbstractFileDownload;
import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.shared.exceptions.ApplicationException;
import org.jepria.compat.shared.exceptions.SystemException;

import java.util.Map;

/**
 * Реализует запись в CLOB.
 */
public class TextFileDownloadImpl extends AbstractFileDownload implements TextFileDownload {
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

    int result;
    try {

      super.largeObject = new TextLargeObject(tableName, fileFieldName, primaryKeyMap);
      result = ((TextLargeObject)super.largeObject).beginRead();
    } catch (ApplicationException ex) {
      cancel();
      throw ex;
    } catch (IllegalStateException ex) {
      ex.printStackTrace();
      throw new SystemException("begin write error", ex);
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

    int result;
    try {

      super.largeObject = new TextLargeObject(tableName, fileFieldName, whereClause);
      result = ((TextLargeObject)super.largeObject).beginRead();
    } catch (ApplicationException ex) {
      cancel();
      throw ex;
    } catch (IllegalStateException ex) {
      ex.printStackTrace();
      throw new SystemException("begin write error", ex);
    }

    return result;
  }
  
  /**
   * Чтение очередного блока данных из CLOB.
   * 
   * @param dataBlock блок данных
   * @throws SpaceException
   */
  @Override
  public int continueRead(char[] dataBlock) throws SpaceException {
    boolean cancelled = true;
    int result = 0;
    try {
      result = ((TextLargeObject)super.largeObject).continueRead(dataBlock);
      cancelled = false;
    } catch (SpaceException e) {
      throw e;
    } catch (Exception e) {
      throw new SpaceException("continue read error", (Exception) e);
    } finally {
      if (cancelled) {
        cancel();
      }
    }
    
    return result;
  }
  
  @Override
  public int beginRead(Object rowId) {
    throw new UnsupportedOperationException();
  }
}
