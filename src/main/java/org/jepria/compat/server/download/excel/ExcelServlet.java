package org.jepria.compat.server.download.excel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.log4j.Logger;
import org.jepria.server.data.RecordDefinition;
import org.jepria.server.service.rest.SearchServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.jepria.compat.server.JepRiaServerConstant.*;
import static org.jepria.compat.shared.util.JepRiaUtil.isEmpty;

/**
 * Сервлет для отображения набора данных в Excel.<br/>
 * Для использования в прикладном модуле необходимо:
 * <ul>
 *   <li>унаследовать в прикладном модуле сервлет от данного класса вызвав в <code>public</code> конструкторе без параметров конструктор данного
 *   класса {@link #ExcelServlet(RecordDefinition recordDefinition)} с указанием
 *   {@link RecordDefinition определения записи}. Пример:
 *     <pre>
 *       ...
 *       public PrintActExcelServlet() {
 *         super(PartnerActRecordDefinition.instance);
 *       }
 *       ...
 *     </pre>
 *   </li>
 *   <li>при необходимости переопределить <code>protected</code>-метод {@link #createExcelReport(List, List, List)}.
 *   </li>
 *   <li>указать в <code>web.xml</code> определение для вызова прикладного сервлета. Пример:
 *     <pre>
 *       ...
 *       &lt;servlet&gt;
 *         &lt;servlet-name&gt;PrintActExcelServlet&lt;/servlet-name&gt;
 *         &lt;servlet-class&gt;com.technology.rfi.outofstaffasria.partneract.server.PrintActExcelServlet&lt;/servlet-class&gt;
 *       &lt;/servlet&gt;
 *       &lt;servlet-mapping&gt;
 *         &lt;servlet-name&gt;PrintActExcelServlet&lt;/servlet-name&gt;
 *         &lt;url-pattern&gt;/OutOfStaffAsRia/printActExcel&lt;/url-pattern&gt;
 *       &lt;/servlet-mapping&gt;
 *       ...
 *     </pre>
 *   </li>
 * </ul>
 * <b>Важно:</b> При добавлении новых полей необходимо внести их в
 * {@link RecordDefinition определение записи}.
 */
@SuppressWarnings("serial")
public class ExcelServlet extends HttpServlet {

  protected static final Logger logger = Logger.getLogger(ExcelServlet.class.getName());

  /**
   * Создает сервлет для отображения набора данных в Excel.<br/>
   * Конструктор вызывается с указанием {@link RecordDefinition определения записи} в прикладных модулях
   * из <code>public</code> конструктора без параметров.
   */
  public ExcelServlet() {}

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Gson gson = new Gson();
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    Map<String, ?> requestBody = gson.fromJson(request.getReader(), type);
    String searchIdParameter = String.valueOf(requestBody.get(SEARCH_ID_PARAMETER));
    if (isEmpty(searchIdParameter)) {
      response.setStatus(400);
      response.getOutputStream().print("Request parameter '" + SEARCH_ID_PARAMETER + "' is mandatory");
      return;
    }

    HttpSession session = request.getSession();

    session.setAttribute(EXCEL_REPORT_HEADERS + searchIdParameter, requestBody.get(EXCEL_REPORT_HEADERS));
    session.setAttribute(EXCEL_REPORT_FIELDS + searchIdParameter, requestBody.get(EXCEL_REPORT_FIELDS));

    response.setStatus(201);
  }

  /**
   * Фабричный метод, формирующий объект Excel-отчёта.<br/>
   * По умолчанию создаёт объект класса ExcelReport. Если в прикладном модуле для этих цедей
   * используется собственный класс, то данный метод необходимо переопределить.
   *
   * @param reportFields  список идентификаторов полей для формирования выгрузки
   * @param reportHeaders список заголовков таблицы в Excel-файле
   * @param records       спиок записей для выгрузки
   * @return объект Excel-отчёта
   */
  protected ExcelReport createExcelReport(List<String> reportFields, List<String> reportHeaders, List<?> records) {
    return new ExcelReport(reportFields, reportHeaders, records);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    logger.trace("BEGIN Generate Excel Report");

    String searchIdParameter = request.getParameter(SEARCH_ID_PARAMETER);
    if (isEmpty(searchIdParameter)) {
      response.setContentType("text/html;charset=UTF-8");
      response.getOutputStream().print("<b>Request parameter '" + SEARCH_ID_PARAMETER + "' is mandatory!</b>");
      return;
    }

    String[] reportHeadersParameter = request.getParameterValues(EXCEL_REPORT_HEADERS);
    String[] reportFieldsParameter = request.getParameterValues(EXCEL_REPORT_FIELDS);

    HttpSession session = request.getSession();

    String fileName = request.getParameter(EXCEL_FILE_NAME_PARAMETER);
    if (fileName == null) {
      fileName = EXCEL_DEFAULT_FILE_NAME;
    }
    logger.trace("fileName=" + fileName);

    @SuppressWarnings("unchecked")
    List<String> reportHeaders = reportHeadersParameter != null ?
        Arrays.asList(reportHeadersParameter)
        : (List<String>) session.getAttribute(EXCEL_REPORT_HEADERS + searchIdParameter);
    logger.trace("reportHeaders = " + reportHeaders);

    @SuppressWarnings("unchecked")
    List<String> reportFields = reportFieldsParameter != null ?
        Arrays.asList(reportFieldsParameter)
        : (List<String>) session.getAttribute(EXCEL_REPORT_FIELDS + searchIdParameter);
    logger.trace("reportFields = " + reportFields);

    @SuppressWarnings("unchecked")
    List<?> records = (List<?>) session.getAttribute("SearchService;searchId=" + searchIdParameter + ";key=rset;");
    logger.trace("resultRecords = " + records);

    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "cache"); //HTTP 1.1
    response.setHeader("Pragma", "no-cache"); //HTTP 1.0
    response.setDateHeader("Expires", 0); //prevents caching at the proxy server
    response.setDateHeader("Last-Modified", System.currentTimeMillis());
    response.setContentType("application/vnd.ms-excel; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

    PrintWriter pw = response.getWriter();

    try {
      ExcelReport report = createExcelReport(
          reportFields,
          reportHeaders,
          records);
      report.print(pw);

      pw.flush();
      response.flushBuffer();
    } catch (Throwable th) {
      onError(response, th);
    }

    logger.trace("END Generate Excel Report");
  }

  /**
   * Отправка сообщения об ошибке в случае её возникновения.<br/>
   * При необходимости данный метод может быть переопределён в классе-наследнике.
   *
   * @param response результат работы сервлета (ответ)
   * @param th       возникшее исключение
   * @throws IOException
   */
  protected void onError(HttpServletResponse response, Throwable th) throws IOException {
    logger.error(th.getMessage(), th);
    response.setContentType("text/html; charset=UTF-8");
    response.setHeader("Content-Disposition", "");
    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, th.getMessage());
  }
}
