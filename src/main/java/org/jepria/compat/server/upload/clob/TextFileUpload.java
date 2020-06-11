package org.jepria.compat.server.upload.clob;

import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.server.upload.FileUpload;

/**
 * Интерфейс выгрузки текстового файла
 */
public interface TextFileUpload extends FileUpload {

  /**
   * Добавление очередного блока данных при записи в CLOB.
   * 
   * @param dataBlock блок данных
   * @throws SpaceException
   */
  void continueWrite(char[] dataBlock) throws SpaceException;
}
