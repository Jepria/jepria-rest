package org.jepria.server.service.rest;

import javax.ws.rs.BadRequestException;
import java.util.Optional;

public class BadParamException extends BadRequestException {

  /**
   * javax.ws.rs-possible method parameter types
   */
  public enum ParamType {
    BEAN, COOKIE, FORM, HEADER, MATRIX, PATH, QUERY,
  }

  /**
   * Type of the bad parameter
   */
  private final ParamType type;

  /**
   * Name of the bad parameter
   */
  private final String name;

  /**
   * Actual value of the bad parameter.
   * {@code null} means unset value, nullable {@code Optional} object means the {@code null} value itself
   */
  private final Optional<Object> actual;

  public ParamType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public Optional<Object> getActual() {
    return actual;
  }


  public BadParamException() {
    this((ParamType)null, null);
  }

  public BadParamException(ParamType type, String name) {
    this.type = type;
    this.name = name;
    this.actual = null;
  }

  /**
   *
   * @param type
   * @param name
   * @param actual passing {@code null} means the {@code null} value itself
   */
  public BadParamException(ParamType type, String name, Object actual) {
    this.type = type;
    this.name = name;
    this.actual = Optional.ofNullable(actual);
  }

  public BadParamException(String message) {
    this(null, null, message);
  }

  public BadParamException(ParamType type, String name, String message) {
    super(message);
    this.type = type;
    this.name = name;
    this.actual = null;
  }

  /**
   *
   * @param type
   * @param name
   * @param actual passing {@code null} means the {@code null} value itself
   * @param message
   */
  public BadParamException(ParamType type, String name, Object actual, String message) {
    super(message);
    this.type = type;
    this.name = name;
    this.actual = Optional.ofNullable(actual);
  }

  public BadParamException(String message, Throwable cause) {
    this(null, null, message, cause);
  }

  public BadParamException(ParamType type, String name, String message, Throwable cause) {
    super(message, cause);
    this.type = type;
    this.name = name;
    this.actual = null;
  }

  /**
   *
   * @param type
   * @param name
   * @param actual passing {@code null} means the {@code null} value itself
   * @param message
   * @param cause
   */
  public BadParamException(ParamType type, String name, Object actual, String message, Throwable cause) {
    super(message, cause);
    this.type = type;
    this.name = name;
    this.actual = Optional.ofNullable(actual);
  }
}
