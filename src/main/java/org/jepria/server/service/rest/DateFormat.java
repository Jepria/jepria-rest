package org.jepria.server.service.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Применяется к параметрам jaxrs-методов <code>@QueryParam</code> или <code>@PathParam</code> с типом java.util.Date для определения формата.
 *
 * <br/><br/>
 * Для определения формата используются выражения, принятые для {@link java.text.SimpleDateFormat}.
 *
 * <br/><br/>
 * По умолчанию (без использования аннотации) в параметрах-датах используется формат ISO 8601: <code>yyyy-MM-dd</code> или <code>yyyy-MM-ddTHH:mm:ssZ</code>.
 *
 * <br/><br/>
 * Пример:
 * <pre>
 * public class Resource {
 *   &#064;GET
 *   &#064;Path("/days/{date}")
 *   public Response get(&#064;PathParam("date") &#064;DateFormat("dd.MM.yyyy") Date date) {
 *     ...
 *   }
 *
 *   &#064;GET
 *   &#064;Path("/time-range")
 *   public Response get(&#064;QueryParam("dateTimeFrom") &#064;DateFormat("dd_MM_yyyy-HH_mm") Date dateTimeFrom,
 *       &#064;QueryParam("dateTimeTo") &#064;DateFormat("dd_MM_yyyy-HH_mm") Date dateTimeTo) {
 *     ...
 *   }
 * }
 * </pre>
 *
 * Описанный сервис принимает запросы в формате:<br/>
 * <code>GET /days/31.12.2010</code><br/>
 * <code>GET /time-range?dateTimeFrom=31_12_2010-23_59&dateTimeTo=01_01_2011-00_01</code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER })
public @interface DateFormat {

  public static final String DEFAULT_FORMAT = "yyyy-MM-dd"; // iso 8601

  String value() default DEFAULT_FORMAT;
}