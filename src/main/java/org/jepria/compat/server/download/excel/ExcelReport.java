package org.jepria.compat.server.download.excel;

import org.jepria.server.data.DtoUtil;
import org.jepria.server.data.RecordDefinition;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;

import static org.jepria.compat.server.JepRiaServerConstant.JEP_RIA_RESOURCE_BUNDLE_NAME;
import static org.jepria.compat.server.download.excel.ExcelReportConstant.*;
import static org.jepria.compat.shared.util.JepRiaUtil.isEmpty;

/**
 * Класс, формирующий Excel-отчёт.<br/>
 * Формирует отчёт в формате Excel XML в соответствии с RecordDefinition, списком записей,
 * списком полей и списком имён полей. Если в прикладном модуле необходимо изменить внешний
 * вид отчёта, то это осуществляется посредством перекрытия <core>protected</code>-методов.
 *
 */
public class ExcelReport {
  
  /**
   * Список идентификаторов полей, используемых для формирования документа.
   */
  protected final List<String> reportFields;
  
  /**
   * Список имён полей (заголовки таблицы в выгружаемом файле).
   */
  protected final List<String> reportHeaders;
  
  /**
   * Список записей, на основе которых строится документ.
   */
  protected final List<?> resultRecords;

  /**
   * Resource bundle (необходим для обработки полей типа Boolean).
   */
  protected final ResourceBundle resourceBundle = ResourceBundle.getBundle(JEP_RIA_RESOURCE_BUNDLE_NAME);
  
  /**
   * Десятичный форат.
   */
  protected final DecimalFormat decimalFormat;
  
  /**
   * Флаг, отвечающий за включение/выключение автофильтра.<br/>
   * По умолчанию автофильтр включён.
   */
  private boolean autoFilter = true;
  
  /**
   * Создаёт Excel-отчёт.
   * @param reportFields список идентификаторов полей, участвующих в формировании документа
   * @param reportHeaders наименования полей
   * @param resultRecords список записей для формирования документа
   */
  public ExcelReport(List<String> reportFields, List<String> reportHeaders, List<?> resultRecords) {
    this.reportFields = reportFields;
    this.reportHeaders = reportHeaders;
    this.resultRecords = resultRecords;
    /*
     * Принудительно используем точку в качестве десятичного разделителя.
     */
    DecimalFormatSymbols decimalSymbols = new DecimalFormatSymbols(Locale.getDefault());
    decimalSymbols.setDecimalSeparator('.');
    decimalFormat = new DecimalFormat(EXCEL_DECIMAL_FORMAT, decimalSymbols); 
  }

  /**
   * Включение или выключение автофильтра.<br/>
   * По умолчанию автофильтр включён.
   * @param autoFilter флаг включения автофильтра
   */
  public void setAutoFilter(boolean autoFilter) {
    this.autoFilter = autoFilter;
  }
  
  /**
   * Возвращает флаг включения автофильтра.<br/>
   * По умолчанию автофильтр включён.
   * @return true, если автофильтр включён, и false в противном случае
   */

  public boolean isAutoFilter() {
    return autoFilter;
  }
  

  /**
   * Вывод документа на печать.
   * @param pw объект класса PrintWriter, в который осуществляется печать документа
   */
  public void print(PrintWriter pw) {
    pw.println(XML_HEADER);
    pw.println(STYLES_HEADER);
    pw.println(createStyles());
    pw.println(STYLES_FOOTER);
    pw.println(TABLE_HEADER);    
      
    if(reportHeaders != null) {
      pw.println(createColumns());
      pw.println(createHeaderRow());
    }
    
    if(resultRecords != null) {
      for(Object record: resultRecords) {
        pw.println(createDataRow(record));
      }
      pw.println(TABLE_FOOTER);
      if (autoFilter) {
        pw.println(createAutoFilter());
      }
      pw.println(XML_FOOTER);
    }
  }

  /**
   * Создание автофильтра. <br/>
   * Возвращает строку автофильтра. По умолчанию в фильтрации участвуют все столбцы. 
   * Если необходим нестандартный автофильтр (например, не по всем столбцам), метод 
   * перекрывается в наследниках.
   * @return XML-представление автофильтра
   */
  protected String createAutoFilter() {
    int columnsCount = reportFields.size();
    int recordsCount = resultRecords.size();
    String range = "R1C1:R" + (recordsCount + 1) + "C" + (columnsCount);
    return AUTO_FILTER_PREFIX + range + AUTO_FILTER_SUFFIX;
  }
  

  /**
   * Создание списка стилей. <br/>
   * Возвращает строку со стилями. Если необходимо определить дополнительные стили или заменить
   *  стили по умолчанию, данный метод перекрывается в наследниках.
   * @return XML-представление стилей документа
   */
  protected String createStyles() {
    return DEFAULT_STYLES;
  }

  /**
   * Создание заголовка таблицы. <br/>
   * Формирует строку с заголовком таблицы. Имена полей обрамляются секцией CDATA. Если необходимо
   *  сделать "нестандартный" заголовок, данный метод переопределяется в классах-наследниках.
   * @return XML-представление заголовочной строки документа
   */
  protected String createHeaderRow() {
    StringBuilder cellsBuilder = new StringBuilder();
    for(String header: reportHeaders) {
      cellsBuilder.append(HEADER_CELL_PREFIX);
      cellsBuilder.append(CDATA_PREFIX);
      cellsBuilder.append(header);
      cellsBuilder.append(CDATA_POSTFIX);
      cellsBuilder.append(CELL_POSTFIX);
    }
    return ROW_PREFIX + cellsBuilder.toString() + ROW_POSTFIX;
  }

  /**
   * Создание столбцов. <br/>
   * Формирует строку с описанием столбцов. По умолчанию задаётся ширина столбцов, равная 150.
   *  Если необходимо изменить количество столбцов, ширину или другие их параметры, данный метод
   *  переопределяется в наследниках.
   * @return XML-представление объявления столбцов
   */
  protected String createColumns() {
    StringBuilder columnsBuilder = new StringBuilder();
    int count = reportHeaders.size();
    for (int i = 0; i < count; i++) {
      columnsBuilder.append(COLUMN_PREFIX);
    }
    return columnsBuilder.toString();
  }

  /**
   * Формирование строки с данными. <br/>
   * При необходимости изменить состав полей, данный метод переопределяется
   * в наследниках.
   * @param record запись, на основе которой формируется строка
   * @return XML-представление строки
   */
  protected String createDataRow(Object record) {
    StringBuilder rowBuilder = new StringBuilder();
    Map<String, ?> recordMap = DtoUtil.dtoToMap(record);
    Map<String, Class<?>> recordTypeMap = dtoToTypeMap(record);
    if(reportFields != null) {
      for(String field: reportFields) {
        String cell = createCell(recordMap, recordTypeMap, field);
        rowBuilder.append(cell);
      }  
    }
    
    return ROW_PREFIX + rowBuilder.toString() + ROW_POSTFIX;
  }

  /**
   * Формирование ячейки на основе записи и имени поля. <br/>
   * Если необходимо индивидуальное форматирование для одного или нескольких полей,
   * данный метод переопределяется в наследниках.
   * @param record запись, из которой берётся поле
   * @param field идентификатор поля
   * @return XML-представление ячейки
   * @throws IllegalStateException в случае, если поле отсутствует в 
   * {@link RecordDefinition определении записи}
   */
  protected String createCell(Map<String, ?> record, Map<String, Class<?>> recordTypeMap, String field) {
    Class<?> type = recordTypeMap.get(field);
        if (type == null) {
            throw new IllegalStateException(
              MessageFormat.format(resourceBundle.getString("errors.excel.fieldTypeNotDefined"), field));
        }
    Object value = record.get(field);
    if (isEmpty(value)) {
      return createEmptyCell();
    } else {
      if (Date.class.equals(type)) {
        if (hasTime((Date) value)) {
          return createDateTimeCell((Date) value);
        } else {
          return createDateCell((Date) value);
        }
      } else if (Boolean.class.equals(type)) {
        return createBooleanCell((Boolean) value);
      } else if (Integer.class.equals(type)) {
        return createIntegerCell((Integer) value);
      } else if (Float.class.equals(type) || Double.class.equals(type) || BigDecimal.class.equals(type)) {
        return createDecimalCell((Number) value);
      }
      return createDefaultCell(value);
    }
  }

  /**
   * Создание пустой ячейки. <br/>
   * Формирование пустой ячейки для полей типа null. Если необходимо изменить вид пустых полей,
   * метод переопределяется в наследниках.
   * @return XML-представление пустой ячейки
   */
  protected String createEmptyCell() {
    return EMPTY_CELL;
  }

  /**
   * Создание ячейки с десятичным числом. <br/>
   * Формирование ячейки с десятичными данными (Float, Double, BigDecimal). По умолчанию
   * выводится два знака после запятой. Если необходимо вывести большее количество знаков
   * или изменить форматирование, метод переопределяется в наследниках.
   * <b>Внимание:</b> В качестве десятичного разделителя обязательно должна выступать точка,
   * иначе документ не будет сформирован корректно.
   * @param number значение, помещаемое в ячейку
   * @return XML-представление ячейки с десятичными данными
   */
  protected String createDecimalCell(Number number) {
    String stringValue = (!isEmpty(number) ? decimalFormat.format(number) : "");
    return DECIMAL_CELL_PREFIX + stringValue + CELL_POSTFIX;
  }

  /**
   * Создание ячейки с десятичным числом <br/>
   * По умолчанию выводится значение, получаемое с помощью метода <code>toString()</code>.
   * При необходимости изменить отображение целочисленных ячеек метод переопределяется в наследниках.
   * @param integerValue целочисленное значение, помещаемое в ячейку
   * @return XML-представление ячейки
   */
  protected String createIntegerCell(Integer integerValue) {
    return INTEGER_CELL_PREFIX + (!isEmpty(integerValue) ? integerValue.toString() : "") + CELL_POSTFIX;
  }

  /**
   * Создание ячейки с данными булевского типа. <br/>
   * По умолчанию, для истинных значений выводится "Да", для ложных "Нет"
   * в соответствующей локали. Для <code>null</code> выводится пустая строка.
   * Используется стиль форматирования строковых ячеек. Если необходимо специальное
   * форматирование или вывод другого текста, метод переопределяется в наследниках.
   * @param booleanValue значение, помещаемое в ячейку
   * @return XML-представление ячейки с булевым значением
   */
  protected String createBooleanCell(Boolean booleanValue) {
    String stringValue;
      if (booleanValue) {
        stringValue = resourceBundle.getString("yes");
      }
      else {
        stringValue = resourceBundle.getString("no");
      }
    return STRING_CELL_PREFIX + stringValue + CELL_POSTFIX;
  }

  /**
   * Determines whether or not a date has any time values.
   * @param date The date.
   * @return true if the date is not null and any of the date's hour, minute,
   * seconds or millisecond values are greater than zero.
   */
  private static boolean hasTime(Date date) {
    if (date == null) {
      return false;
    }
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    if (c.get(Calendar.HOUR_OF_DAY) > 0) {
      return true;
    }
    if (c.get(Calendar.MINUTE) > 0) {
      return true;
    }
    if (c.get(Calendar.SECOND) > 0) {
      return true;
    }
    if (c.get(Calendar.MILLISECOND) > 0) {
      return true;
    }
    return false;
  }

  /**
   * Создание ячейки с датой.<br/>
   * Если необходимо специальное форматирование для ячеек с датой, данный метод
   * переопределяется в наследниках.
   * @param date дата, помещаемая в ячейку
   * @return XML-представление ячейки с датой
   */
  protected String createDateCell(Date date) {
    DateFormat dateFormat = new SimpleDateFormat(EXCEL_DATE_FORMAT);
    String stringValue = dateFormat.format(date);
    return DATE_CELL_PREFIX + stringValue + CELL_POSTFIX;
  }

  /**
   * Создание ячейки с датой и временем.<br/>
   * Если необходимо специальное форматирование для ячеек с датой и временем, данный метод
   * переопределяется в наследниках.
   * @param dateTime дата и время, помещаемые в ячейку
   * @return XML-представление ячейки с датой и временем
   */
  protected String createDateTimeCell(Date dateTime) {
    DateFormat dateTimeFormat = new SimpleDateFormat(EXCEL_DATETIME_FORMAT);
    String stringValue = dateTimeFormat.format(dateTime);
    return DATETIME_CELL_PREFIX + stringValue + CELL_POSTFIX;
  }

  /**
   * Создание ячейки для полей всех прочих типов.<br/>
   * По умолчанию с в ячейку помещается строковое представление объекта, 
   * получаемое с помощью метода <code>toString()</code>, обрамлённое секцией CDATA.
   * @param value объект, строковое представление которого необходимо поместить в ячейку
   * @return XML-представление ячейки
   */
  protected String createDefaultCell(Object value) {
    String stringValue = value.toString();
    return STRING_CELL_PREFIX + CDATA_PREFIX + stringValue
        + CDATA_POSTFIX + CELL_POSTFIX;
  }

  //TODO убрать когда recordDefinition поддержит полный fieldTypeMap
  private static Map<String, Class<?>> dtoToTypeMap(Object dto) {
    if (dto == null) {
      return null;
    }
    Map<String, Class<?>> fieldsMap = new HashMap<>();
    Field[] allFields = dto.getClass().getDeclaredFields();
    for (Field field : allFields) {
      if (!field.isAccessible()) {
        field.setAccessible(true);
      }
      try {
        Object value = field.get(dto);
        fieldsMap.put(field.getName(), value.getClass());
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
    }
    return fieldsMap;
  }

}
