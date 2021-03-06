package org.jepria.compat.server.download.blob;

import org.jepria.compat.server.download.FileDownload;
import org.jepria.compat.server.exceptions.SpaceException;

/**
 * Интерфейс выгрузки бинарного файла.
 */
public interface BinaryFileDownload extends FileDownload {
  /**
   * Чтение очередного блока данных из BINARY_FILE.
   * 
   * @param dataBlock блок данных
   * @throws SpaceException
   */
  int continueRead(byte[] dataBlock) throws SpaceException;
}
