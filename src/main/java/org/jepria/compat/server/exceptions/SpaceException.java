package org.jepria.compat.server.exceptions;

import org.jepria.compat.shared.exceptions.ApplicationException;

/**
 * Инициируется при обнаружении недостатка каких-либо ресурсов.
 */
public class SpaceException extends ApplicationException {
  private static final long serialVersionUID = -705921017428418985L;

  public SpaceException(String message, Throwable cause) {
    super(message, cause);
  }

}
