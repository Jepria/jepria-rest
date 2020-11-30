package org.jepria.compat.server;

import java.nio.charset.Charset;

import org.jepria.compat.shared.JepRiaConstant;

public class JepRiaServerConstant extends JepRiaConstant {
  
  public static final String LOCALE_KEY = "org.jepria.compat.server.LOCALE";
  
  public static final String JEP_RIA_RESOURCE_BUNDLE_NAME = "org.jepria.server.text.JepRiaText";
    
  /**
   * JNDI-имя источника данных модуля.
   */
  public static final String DEFAULT_DATA_SOURCE_JNDI_NAME = "jdbc/RFInfoDS";
  /**
   * JNDI-имя источника данных модуля OAuth.
   */
  public static final String DEFAULT_OAUTH_DATA_SOURCE_JNDI_NAME = "jdbc/OAuthDS";
  public static final String BACK_UP_DATA_SOURCE = "OAUTH_BACKUP_DATASOURCE";
  /**
   * Имя параметра http-запроса: язык текущей межмодульной сессии
   */
  public static final String HTTP_REQUEST_PARAMETER_LANG = "lang";
 
  /**
   * Префикс имени аттрибута сессии, в котором сохраняется найденный набор записей. 
   */
  public static final String FOUND_RECORDS_SESSION_ATTRIBUTE = "foundRecordsSessionAttribute";

  public static final String SEARCH_ID_PARAMETER = "searchId";

  /**
   * Префикс имени аттрибута сессии, в котором сохраняется список содержащий названия колонок Excel-отчета.
   */
  public static final String EXCEL_REPORT_HEADERS = "excelReportHeaders";

  /**
   * Префикс имени аттрибута сессии, в котором сохраняется список содержащий идентификаторы полей, из которых брать данные для колонок Excel-отчета.
   */
  public static final String EXCEL_REPORT_FIELDS = "excelReportFields";
  
  /**
   * Имя атрибута сессии, в котором хранится флаг автообновления.
   */
  public static final String IS_REFRESH_NEEDED = "isRefreshNeeded";
  
  /**
   * Суффикс, подставляемый в логин для указания того, что авторизация будет осуществлена по хэшу пароля
   */
  public static final String LOGIN_SUFFIX_FOR_HASH_AUTHORIZATION = String.valueOf((char) 2).concat("hashed");
  

  /**
   * Внешнее свойство, в котором передается адрес сервера CAS
   */
  public static final String CAS_SERVER_ADDRESS_PROPERTY = "CAS_SERVER_ADDRESS";
  

  /**
   * Имя параметра приложения <I>context-param</I> web.xml, содержащего адрес соответствующего CAS-сервера
   */
  public static final String CAS_SERVER_NAME_CONTEXT_PARAMETER = "casServerName";
  
  /**
   * Кодировка по умолчанию: UTF-8.
   */
  public static final Charset DEFAULT_ENCODING = Charset.forName("UTF-8");
}
