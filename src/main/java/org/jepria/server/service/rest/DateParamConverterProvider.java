package org.jepria.server.service.rest;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Date;

public class DateParamConverterProvider implements ParamConverterProvider {
  @Override
  public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
    if (Date.class.equals(type)) {
      final DateParamConverter dateParamConverter =
              new DateParamConverter();

      for (Annotation annotation : annotations) {
        if (DateFormat.class.equals(
                annotation.annotationType())) {
          dateParamConverter.
                  setCustomDateFormat((DateFormat) annotation);
        }
      }

      return (ParamConverter<T>) dateParamConverter;
    }
    return null;
  }
}
