package org.jepria.server.service.rest;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ParamConverter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateParamConverter implements ParamConverter<Date> {

  protected DateFormat customDateFormat;

  public void setCustomDateFormat(DateFormat customDateFormat) {
    this.customDateFormat = customDateFormat;
  }

  @Override
  public Date fromString(String s) {

    if (s == null) {
      return null;
    }
    
    final String format;

    if (customDateFormat != null) {
      format = customDateFormat.value();
    } else {
      format = DateFormat.DEFAULT_FORMAT;
    }

    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);

    try {
      return simpleDateFormat.parse(s);
    } catch (ParseException e) {
      throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incorrect date or date-time format: expected [" + format + "], actual [" + s + "]").build(),
              e);
    }
  }
  @Override
  public String toString(Date date) {
    // TODO why to invoke this?
    throw new UnsupportedOperationException();
  }
}
