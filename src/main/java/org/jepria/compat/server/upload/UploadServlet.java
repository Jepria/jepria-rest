package org.jepria.compat.server.upload;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.InvalidFileNameException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.jepria.compat.server.JepRiaServerConstant;
import org.jepria.compat.server.upload.blob.BinaryFileUploadImpl;
import org.jepria.compat.server.upload.blob.FileUploadStream;
import org.jepria.compat.server.upload.clob.FileUploadWriter;
import org.jepria.compat.server.upload.clob.TextFileUploadImpl;
import org.jepria.compat.server.util.JepServerUtil;
import org.jepria.compat.shared.exceptions.ApplicationException;
import org.jepria.compat.shared.exceptions.UnsupportedException;
import org.jepria.compat.shared.util.JepRiaUtil;
import org.jepria.server.data.RecordIdParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static org.jepria.compat.server.JepRiaServerConstant.JEP_RIA_RESOURCE_BUNDLE_NAME;
import static org.jepria.compat.shared.JepRiaConstant.*;

/**
 * Сервлет для загрузки файлов на сервер.<br/>
 * Осуществляет загрузку текстовых и бинарных файлов в таблицу базы данных.
 */
@SuppressWarnings("serial")
public class UploadServlet extends HttpServlet {
  /**
   * Логгер.
   */
  protected static Logger logger = Logger.getLogger(UploadServlet.class.getName());
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
   * Признак необходимости производить загрузку в отдельной транзакции.
   */
  private final boolean transactionable;
  /**
   * Имя поля в таблице
   */
  private final Map<String, String> fileFieldNameMap;
  /**
   * Имя таблицы
   */
  private final String tableName;
  /**
   * Тип файла
   */
  private final Map<String, String> fileTypeMap;

  /**
   * Создаёт сервлет загрузки файлов на сервер.<br>
   * Для текстовых файлов используется кодировка по умолчанию - UTF-8.
   *
   * @param dataSourceJndiName JNDI-наименование источника данных
   */
  public UploadServlet(
      String tableName,
      Map<String, String> fileFieldNames,
      Map<String, String> fileFieldTypes,
      String dataSourceJndiName) {
    this(tableName, fileFieldNames, fileFieldTypes, dataSourceJndiName, JepRiaServerConstant.DEFAULT_ENCODING, true);
  }

  /**
   * Создаёт сервлет загрузки файлов на сервер.
   *
   * @param dataSourceJndiName JNDI-наименование источника данных
   * @param textFileCharset    кодировка текстовых файлов
   */
  public UploadServlet(
      String tableName,
      Map<String, String> fileFieldNames,
      Map<String, String> fileFieldTypes,
      String dataSourceJndiName,
      Charset textFileCharset) {

    this(tableName, fileFieldNames, fileFieldTypes, dataSourceJndiName, textFileCharset, true);
  }

  /**
   * Создаёт сервлет загрузки файлов на сервер.
   *
   * @param dataSourceJndiName JNDI-наименование источника данных
   * @param textFileCharset    кодировка текстовых файлов
   */
  public UploadServlet(
      String tableName,
      Map<String, String> fileFieldNames,
      Map<String, String> fileFieldTypes,
      String dataSourceJndiName,
      Charset textFileCharset,
      boolean transactionable) {

    this.dataSourceJndiName = dataSourceJndiName;
    this.textFileCharset = textFileCharset;
    this.transactionable = transactionable;
    this.fileTypeMap = fileFieldTypes;
    this.tableName = tableName;
    this.fileFieldNameMap = fileFieldNames;
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

  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    logger.debug("doPost()");
    if (ServletFileUpload.isMultipartContent(request)) {
      // Создание фабрики элементов дисковых файлов
      FileItemFactory factory = new DiskFileItemFactory();
      // Создание обработчика загрузки файла
      ServletFileUpload upload = new ServletFileUpload(factory);
      try {
        // Последовательный upload
        // TODO Параллельный эффективнее, но насколько это актуально ?
        List<FileItem> items = upload.parseRequest(request);
        if (items.size() >= 2) { // Должно быть два параметра: файл и привязка к полю записи таблицы БД

          FileItem fileSizeFormField = getFormField(items, FILE_SIZE_HIDDEN_FIELD_NAME);
          String fileSizeAsString = fileSizeFormField != null ? fileSizeFormField.getString() : null;
          Integer specifiedFileSize = JepRiaUtil.isEmpty(fileSizeAsString) ? null : Integer.decode(fileSizeAsString) * 1024; // in Kbytes

          StringBuilder errorMessage = new StringBuilder();

          for (FileItem fileItem : items) {
            // check file size if necessary
            long fileSize = fileItem.getSize();
            if (!JepRiaUtil.isEmpty(specifiedFileSize) && fileSize > specifiedFileSize) {
              ResourceBundle resource = ResourceBundle.getBundle(JEP_RIA_RESOURCE_BUNDLE_NAME);
              response.setCharacterEncoding("UTF-8");
              response.setContentType("text/plain; charset=UTF-8");
              response.getWriter().print(MessageFormat.format(resource.getString("errors.file.uploadFileSizeError"), specifiedFileSize, fileSize));
              response.flushBuffer();
              return;
            }

            String fileName = null;
            try {
              fileName = fileItem.getName();
            } catch (InvalidFileNameException e) {
              fileName = e.getName();
            }

            FileItem isDeletedFormField = getFormField(items, IS_DELETED_FILE_HIDDEN_FIELD_NAME);
            boolean isDeleted = isDeletedFormField != null &&
                Boolean.valueOf(isDeletedFormField.getString());
            String fieldName = this.fileFieldNameMap.get(fileItem.getFieldName());
            String fileType = this.fileTypeMap.get(fileItem.getFieldName());

            if (!JepRiaUtil.isEmpty(fileName) || isDeleted) {
              try {
                if (fileType == BINARY_FILE) {
                  uploadBinary(
                      fileItem.getInputStream(),
                      tableName,
                      fieldName,
                      getPrimaryKeyMap(items));
                } else if (fileType == TEXT_FILE) {
                  uploadText(
                      fileItem.getInputStream(),
                      tableName,
                      fieldName,
                      getPrimaryKeyMap(items));
                } else {
                  throw new UnsupportedException(this.getClass() + ".doPost(): " + fileType + " field type does not supported for upload.");
                }
              } catch (Throwable th) {
                String message = "doPost().upload error in '" + fieldName + "' field: " + th.getMessage();
                logger.error(message, th);
                errorMessage.append(message);
                errorMessage.append("\n");
              }
            }
          }

          response.setStatus(HttpServletResponse.SC_CREATED);
          if (errorMessage.length() == 0) {
            response.getWriter().print("Upload success");
          } else {
            response.getWriter().print(errorMessage);
          }
          response.flushBuffer();
        } else {
          FileItem formField = getFormField(items, PRIMARY_KEY_HIDDEN_FIELD_NAME);
          // Параметр привязки к полю записи таблицы БД
          throw new IllegalArgumentException("Parameters number should be 3, but we have: "
              + items.size() + ". "
              + (formField == null ? "FormField" : "File")
              + " item is absent.");

        }
      } catch (Exception ex) {
        ex.printStackTrace();
        onError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "doPost(): " + "Upload error: " + ex.getMessage());
      }
    } else {
      onError(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
          "doPost(): Request contents type is not supported by the servlet.");
    }
  }

  /**
   * Возвращает карту первичных ключей.
   *
   * @param items Список FileItem из запроса.
   * @return Карта первичных ключей.
   * @throws UnsupportedEncodingException
   * @throws ApplicationException
   */
  protected Map<String, String> getPrimaryKeyMap(List<FileItem> items) throws UnsupportedEncodingException, ApplicationException {
    // Параметр привязки к полю записи таблицы БД
    FileItem primaryKeyFormField = getFormField(items, PRIMARY_KEY_HIDDEN_FIELD_NAME);
    String primaryKeyString = primaryKeyFormField.getString("UTF-8");
    Map<String, String> primaryKeyMap = RecordIdParser.parseComposite(primaryKeyString);
    return primaryKeyMap;
  }

  /**
   * Загрузка на сервер бинарного файла.
   *
   * @param inputStream   поток данных
   * @param tableName     имя таблицы, в которую загружается файл
   * @param fileFieldName имя поля, в которое загружается файл
   * @param primaryKeyMap первичный ключ
   * @throws IOException
   * @throws Exception
   */
  protected void uploadBinary(
      InputStream inputStream
      , String tableName
      , String fileFieldName
      , Map<String, String> primaryKeyMap
  ) throws IOException, Exception {

    FileUploadStream.uploadFile(
        inputStream,
        new BinaryFileUploadImpl(),
        tableName,
        fileFieldName,
        primaryKeyMap,
        this.dataSourceJndiName,
        this.moduleName,
        transactionable);

  }

  /**
   * Загрузка на сервер текстового файла.
   *
   * @param inputStream   поток данных
   * @param tableName     имя таблицы, в которую загружается файл
   * @param fileFieldName имя поля, в которое загружается файл
   * @param primaryKeyMap первичный ключ
   * @throws IOException
   * @throws Exception
   */
  protected void uploadText(
      InputStream inputStream
      , String tableName
      , String fileFieldName
      , Map<String, String> primaryKeyMap
  ) throws IOException, Exception {
    FileUploadWriter.uploadFile(
        new InputStreamReader(inputStream, textFileCharset),
        new TextFileUploadImpl(),
        tableName,
        fileFieldName,
        primaryKeyMap,
        this.dataSourceJndiName,
        this.moduleName,
        transactionable);
  }

  /**
   * Получение интерфейса выгрузки файлов.<br/>
   * Предполагается, что может быть только одно поле формы с указанным наименованием
   *
   * @param items     список интерфейсов выгрузки файлов
   * @param fieldName наименование поля
   */
  protected FileItem getFormField(List<FileItem> items, String fieldName) {
    for (FileItem item : items) {
      if (item.isFormField() && fieldName.equalsIgnoreCase(item.getFieldName())) {
        return item;
      }
    }
    return null;
  }

  /**
   * Отправка сообщения об ошибке в случае неуспешной загрузки.<br/>
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
