package org.jepria.compat.server.upload.blob;

import org.jepria.compat.server.exceptions.SpaceException;
import org.jepria.compat.server.upload.FileUpload;

/**
 * Интерфейс загрузки бинарного файла
 */
public interface BinaryFileUpload extends FileUpload {

  /**
   * Добавление очередного блока данных при записи в BINARY_FILE.
   * 
   * @param dataBlock блок данных
   * @throws SpaceException
   */
  void continueWrite(byte[] dataBlock) throws SpaceException;
}
