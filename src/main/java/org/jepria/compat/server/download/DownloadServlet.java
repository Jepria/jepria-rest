package org.jepria.compat.server.download;

import org.apache.log4j.Logger;
import org.jepria.compat.server.JepRiaServerConstant;
import org.jepria.compat.server.download.blob.BinaryFileDownloadImpl;
import org.jepria.compat.server.download.blob.FileDownloadStream;
import org.jepria.compat.server.download.clob.FileDownloadReader;
import org.jepria.compat.server.download.clob.TextFileDownloadImpl;
import org.jepria.compat.server.util.JepServerUtil;
import org.jepria.compat.shared.exceptions.UnsupportedException;
import org.jepria.compat.shared.util.JepRiaUtil;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jepria.compat.shared.JepRiaConstant.*;

@SuppressWarnings("serial")
public class DownloadServlet extends HttpServlet {
  /**
   * Логгер.
   */
  protected static Logger logger = Logger.getLogger(DownloadServlet.class.getName());
  /**
   * Символы, недопустимые в имени файла.
   */
  private static final char[] illegalCharacters = "\\/:*?\"<>|\t\n".toCharArray();
  /**
   * JNDI-имя источника данных.
   */
  private String dataSourceJndiName;
  /**
   * Имя модуля, передаваемое в DB.
   */
  private String moduleName;
  /**
   * Кодировка текстовых файлов.
   */
  private final Charset textFileCharset;
  /**
   * Имя поля в таблице
   */
  private final String fileFieldName;
  /**
   * Имя таблицы
   */
  private final String tableName;
  /**
   * Тип файла
   */
  private final String fileType;
  /**
   * Имена полей первичного ключа
   */
  private final List<String> primaryKeyFieldNames;

  /**
   * Кодирует строку в формат, подходящий для помещения в заголовок Content-disposition.<br>
   * См. здесь: http://stackoverflow.com/a/611117
   *
   * @param str кодируемая строка
   * @return строка после кодирования
   */
  private static String encodeURIComponent(String str) {
    String result = null;
    try {
      result = URLEncoder.encode(str, "UTF-8")
          .replaceAll("\\+", "%20")
          .replaceAll("\\%21", "!")
          .replaceAll("\\%27", "'")
          .replaceAll("\\%28", "(")
          .replaceAll("\\%29", ")")
          .replaceAll("\\%7E", "~");
    } catch (UnsupportedEncodingException e) {
      // Данное исключение не будет выброшено никогда.
    }
    return result;
  }

  /**
   * Заменяет недопустимые символы знаком подчёркивания.
   *
   * @param str строка
   * @return строка с заменёнными недопустимыми символами
   */
  private static String replaceIllegalCharacters(String str) {
    for (char ch : illegalCharacters) {
      str = str.replace(ch, '_');
    }
    return str;
  }

  /**
   * Создаёт сервлет для загрузки файлов с сервера.
   *
   * @param dataSourceJndiName   JNDI-имя источника данных
   * @param textFileCharset      кодировка текстовых файлов
   */
  public DownloadServlet(
      String tableName,
      String fileFieldName,
      List<String> primaryKeyFieldNames,
      String fileType,
      String dataSourceJndiName,
      Charset textFileCharset) {
    this.dataSourceJndiName = dataSourceJndiName;
    this.textFileCharset = textFileCharset;
    this.fileType = fileType;
    this.primaryKeyFieldNames = Collections.unmodifiableList(primaryKeyFieldNames);
    this.tableName = tableName;
    this.fileFieldName = fileFieldName;
  }

  /**
   * Создаёт сервлет для загрузки файлов с сервера.<br/>
   * Для текстовых файлов используется кодировка по умолчанию - UTF-8.
   *
   * @param dataSourceJndiName   JNDI-имя источника данных
   */
  public DownloadServlet(
      String tableName,
      String fileFieldName,
      List<String> primaryKeyFieldNames,
      String fileType,
      String dataSourceJndiName) {

    this(tableName, fileFieldName, primaryKeyFieldNames, fileType, dataSourceJndiName, JepRiaServerConstant.DEFAULT_ENCODING);
  }

  /**
   * Инициализация сервлета.
   * Переопределение метода обусловлено установкой имени модуля. Выполнить данную операцию
   * в конструкторе невозможно, т.к. <code>getServletContext()</code> в конструкторе
   * выбрасывает <code>NullPointerException</code>.
   */
  @Override
  public void init() throws ServletException {
    super.init();
    moduleName = JepServerUtil.getModuleName(getServletConfig());
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    logger.debug("doGet() request.getQueryString() = " + request.getQueryString());

    try {
      response.reset();

      String downloadId = request.getParameter(DOWNLOAD_ID);
      String fileName, fileNamePrefix, fileExtension, mimeType, contentDisposition, fieldName, fileType;
      List<String> recordKey;
      if (downloadId != null) {
        HttpSession session = request.getSession();
        fileName = (String) session.getAttribute(DOWNLOAD_FILE_NAME + downloadId);
        fileExtension = (String) session.getAttribute(DOWNLOAD_EXTENSION + downloadId);
        fileNamePrefix = (String) session.getAttribute(DOWNLOAD_FILE_NAME_PREFIX + downloadId);
        mimeType = (String) session.getAttribute(DOWNLOAD_MIME_TYPE + downloadId);
        fileType = (String) session.getAttribute(DOWNLOAD_FILE_TYPE + downloadId);
        recordKey = (List<String>) session.getAttribute(DOWNLOAD_RECORD_KEY + downloadId);
        fieldName = (String) session.getAttribute(DOWNLOAD_FIELD_NAME + downloadId);
        contentDisposition = (String) session.getAttribute(DOWNLOAD_CONTENT_DISPOSITION + downloadId);
      } else {
        fileName = request.getParameter(DOWNLOAD_FILE_NAME);
        fileNamePrefix = request.getParameter(DOWNLOAD_FILE_NAME_PREFIX);
        fileExtension = request.getParameter(DOWNLOAD_EXTENSION);
        fileType = request.getParameter(DOWNLOAD_FILE_TYPE);
        mimeType = request.getParameter(DOWNLOAD_MIME_TYPE);
        recordKey = Arrays.asList(request.getParameterValues(DOWNLOAD_RECORD_KEY));
        fieldName = request.getParameter(DOWNLOAD_FIELD_NAME);
        contentDisposition = request.getParameter(DOWNLOAD_CONTENT_DISPOSITION);
      }

      fieldName = fieldName != null ? fieldName : this.fileFieldName;
      fileType = fileType != null ? fileType : this.fileType;
      response.setContentType(mimeType + ";charset=" + textFileCharset);
      addAntiCachingHeaders(response);

      if (DOWNLOAD_CONTENT_DISPOSITION_INLINE.equals(contentDisposition)) {
        response.setHeader("Content-disposition", "inline");
      } else if (DOWNLOAD_CONTENT_DISPOSITION_ATTACHMENT.equals(contentDisposition)) {
        setAttachedFileName(response, fileName, fileExtension, fileNamePrefix, recordKey.stream().toArray(String[]::new).toString());
      }

      ServletOutputStream outputStream = response.getOutputStream();

      if (fileType.equals(BINARY_FILE)) {
        downloadBinary(outputStream,
            tableName,
            fieldName,
            recordKey);
      } else if (fileType.equals(TEXT_FILE)) {
        downloadText(outputStream,
            tableName,
            fieldName,
            recordKey);
      } else {
        throw new UnsupportedException(this.getClass() + ".doGet(): " + fileType + " field type does not supported for download.");
      }
    } catch (Throwable th) {
      logger.error("doGet() threw exception: ", th);
      onError(response,
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "doGet(): " + "Download error: " + th.getMessage());
    }
  }

  /**
   * Установка имени выгружаемого файла (в случае, если файл выгружается как вложение).<br>
   * Имя файла кодируется; недопустимые символы заменяются знаками подчёркивания.
   *
   * @param response       ответ сервлета
   * @param fileName       имя файла
   * @param fileExtension  расширение
   * @param fileNamePrefix префикс имени файла
   * @param recordKey      ключ записи
   * @throws UnsupportedEncodingException
   */
  protected void setAttachedFileName(HttpServletResponse response, String fileName, String fileExtension,
                                     String fileNamePrefix, String recordKey) throws UnsupportedEncodingException {
    String downloadFileName;
    if (!JepRiaUtil.isEmpty(fileName)) {
      downloadFileName = fileName;
    } else if (!JepRiaUtil.isEmpty(fileNamePrefix)) {
      downloadFileName = fileNamePrefix + "_" + recordKey;
    } else {
      downloadFileName = recordKey;
    }

    if (!JepRiaUtil.isEmpty(fileExtension)) {
      fileExtension = "." + fileExtension;
    } else {
      fileExtension = "";
    }

    response.setHeader("Content-disposition", "attachment; filename*=UTF-8''" + encodeURIComponent(replaceIllegalCharacters(downloadFileName)) + replaceIllegalCharacters(fileExtension) + "");
  }

  /**
   * Добавление &quot;типового набора&quot; заголовков для борьбы с кэшированием в разных браузерах.
   *
   * @param response ответ сервлета
   */
  private static void addAntiCachingHeaders(HttpServletResponse response) {
    response.setHeader("Cache-Control", "cache"); //HTTP 1.1
    response.setHeader("Pragma", "no-cache"); //HTTP 1.0
    response.setDateHeader("Expires", 0);
    response.setDateHeader("Last-Modified", System.currentTimeMillis());
  }

  /**
   * Загрузка бинарного файла из базы данных.
   *
   * @param outputStream  выходной поток
   * @param tableName     имя таблицы
   * @param fileFieldName имя поля в таблице
   * @param recordKey     ключ записи
   * @throws IOException
   * @throws NamingException
   */
  private void downloadBinary(
      OutputStream outputStream
      , String tableName
      , String fileFieldName
      , List recordKey
  ) throws IOException, NamingException {

    FileDownloadStream.downloadFile(
        outputStream,
        new BinaryFileDownloadImpl(),
        tableName,
        fileFieldName,
        primaryKeyFieldNames,
        recordKey,
        this.dataSourceJndiName,
        this.moduleName,
        true);
  }

  /**
   * Загрузка текстового файла из базы данных.
   *
   * @param outputStream  выходной поток
   * @param tableName     имя таблицы
   * @param fileFieldName имя поля
   * @param recordKey     ключ записи
   * @throws IOException
   * @throws NamingException
   */
  private void downloadText(
      OutputStream outputStream
      , String tableName
      , String fileFieldName
      , List recordKey
  ) throws IOException, NamingException {

    FileDownloadReader.downloadFile(
        new OutputStreamWriter(outputStream, textFileCharset),
        new TextFileDownloadImpl(),
        tableName,
        fileFieldName,
        primaryKeyFieldNames,
        recordKey,
        this.dataSourceJndiName,
        this.moduleName,
        true);
  }

  /**
   * Отправка сообщения об ошибке в случае ошибки при загрузке файла.<br/>
   * При необходимости данный метод может быть переопределён в классе-наследнике.
   *
   * @param response результат работы сервлета (ответ)
   * @param error    HTTP-код ошибки
   * @param message  текст сообщения об ошибке
   * @throws IOException
   */
  protected void onError(HttpServletResponse response, int error, String message) throws IOException {
    logger.error(message);
    response.sendError(error, message);
  }
}
