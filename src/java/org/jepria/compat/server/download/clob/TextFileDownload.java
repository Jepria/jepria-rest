package org.jepria.compat.server.download.clob;

import org.jepria.compat.server.download.FileDownload;
import org.jepria.compat.server.exceptions.SpaceException;

/**
 * Интерфейс загрузки текстового файла.
 */
public interface TextFileDownload extends FileDownload {
  /**
   * Чтение очередного блока данных из CLOB.
   * 
   * @param dataBlock блок данных
   * @throws SpaceException
   */
  int continueRead(char[] dataBlock) throws SpaceException;
}
