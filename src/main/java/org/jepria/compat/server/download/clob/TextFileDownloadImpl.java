package org.jepria.compat.server.download.clob;

import org.jepria.compat.server.db.clob.TextLargeObject;
import org.jepria.compat.server.download.AbstractFileDownload;
import org.jepria.compat.server.dao.CallContext;
import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.shared.exceptions.ApplicationException;
import org.jepria.compat.shared.exceptions.SystemException;

import java.util.List;

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
      , List<String> primaryKey
      , List<Object> rowIds
      )
      throws ApplicationException {

    int result = -1;
    try {

      super.largeObject = new TextLargeObject(tableName, fileFieldName, primaryKey, rowIds);
      result = ((TextLargeObject)super.largeObject).beginRead();
    } catch (ApplicationException ex) {
      cancel();
      throw ex;
    } catch (IllegalStateException ex) {
      ex.printStackTrace();
      throw new SystemException("begin write error", ex);
    } finally {
      storedContext = CallContext.detach();
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
    CallContext.attach(storedContext);
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
      storedContext = CallContext.detach();
    }
    
    return result;
  }
}
