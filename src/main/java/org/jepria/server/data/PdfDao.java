package org.jepria.server.data;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.fill.*;
import net.sf.jasperreports.engine.type.PrintOrderEnum;
import net.sf.jasperreports.engine.util.FileBufferedOutputStream;
import net.sf.jasperreports.engine.util.JRSwapFile;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Creates and prints PDF files 
 */
public class PdfDao {

  /**
   * Create and print a jasperreport PDF file
   * @param jasperReportInputStream {@code .jasper} compiled report resource stream
   * @param parameters data to fill the report with
   * @param records data to fill the report with
   * @param out stream to print the resultant PDF to
   */
  public static void print(InputStream jasperReportInputStream, Map<String, Object> parameters, List<?> records, OutputStream out) {
    print(jasperReportInputStream, parameters, records, null, out, true);
  }
  
  /**
   * Create and print a jasperreport PDF file
   * @param jasperReportInputStream {@code .jasper} compiled report resource stream
   * @param parameters data to fill the report with
   * @param records data to fill the report with
   * @param locale 
   * @param out stream to print the resultant PDF to
   * @param isBuffered
   */
  public static void print(InputStream jasperReportInputStream, Map<String, Object> parameters, List<?> records, Locale locale, OutputStream out, boolean isBuffered) {
    JasperPrint jasperPrint = prepareJasperPrint(jasperReportInputStream, parameters, records, locale);
    try {
      print(jasperPrint, out, isBuffered);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static JasperPrint prepareJasperPrint(InputStream jasperReportInputStream, Map<String, Object> parameters, List<?> records, Locale locale) {
    JepReportRecords reportRecords = new JepReportRecords(records);
    JRDataSource jrDataSource = new JepReportDataSource(reportRecords);

    if (locale != null) {
      parameters.put("REPORT_LOCALE", locale);
    }

    JRSwapFile swapFile = new JRSwapFile("C:\\apache_tomcat_8.5.46\\work\\Catalina\\localhost\\ApplicationRejectTracking", 1024, 1024);
    JRAbstractLRUVirtualizer virtualizer = new JRSwapFileVirtualizer(2, swapFile, true);
    parameters.put("REPORT_VIRTUALIZER", virtualizer);

    final JasperPrint jasperPrint;
    try {
      jasperPrint = JasperFillManager.fillReport(jasperReportInputStream, parameters, jrDataSource);
    } catch (JRException e) {
      throw new RuntimeException(e);
    }

    removeBlankPage(jasperPrint.getPages());
    virtualizer.setReadOnly(true);
    
    return jasperPrint;
  }

  protected static void print(JasperPrint jasperPrint, OutputStream out, boolean isBuffered) throws IOException {
    if (isBuffered) {
      FileBufferedOutputStream fbos = new FileBufferedOutputStream();
      JRPdfExporter exporter = new JRPdfExporter();
      exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
      exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, fbos);

      try {
        exporter.exportReport();
        fbos.close();
        if (fbos.size() > 0) {
          try {
            fbos.writeData(out);
            fbos.dispose();
            out.flush();
          } finally {
            if (out != null) {
              out.close();
            }

          }
        }
      } catch (JRException var45) {
        throw new RuntimeException(var45);
      } finally {
        fbos.close();
        fbos.dispose();
      }
    } else {
      JRPdfExporter exporter = new JRPdfExporter();
      exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
      exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, out);

      try {
        exporter.exportReport();
      } catch (JRException e) {
        throw new RuntimeException(e);
      } finally {
        if (out != null) {
          out.close();
        }
      }
    }
  }

  protected static void removeBlankPage(List<JRPrintPage> pages) {
    Iterator<JRPrintPage> i = pages.iterator();

    while(i.hasNext()) {
      JRPrintPage page = (JRPrintPage)i.next();
      List<JRPrintElement> elements = page.getElements();
      int size = elements.size();
      Object element = null;
      JRTemplatePrintText jrTemplatePrintText;
      JRTemplatePrintFrame jrTemplatePrintFrame;
      switch(size) {
        case 0:
          i.remove();
          break;
        case 1:
          element = elements.get(0);
          if (element instanceof JRTemplatePrintText) {
            jrTemplatePrintText = (JRTemplatePrintText)element;
            if ("Background".equals(jrTemplatePrintText.getKey())) {
              i.remove();
            }
          } else if (element instanceof JRTemplatePrintFrame) {
            jrTemplatePrintFrame = (JRTemplatePrintFrame)element;
            if (jrTemplatePrintFrame.getElements().size() == 0) {
              i.remove();
            }
          }
          break;
        case 2:
          element = elements.get(0);
          if (element instanceof JRTemplatePrintRectangle) {
            element = elements.get(1);
            if (element instanceof JRTemplatePrintText) {
              jrTemplatePrintText = (JRTemplatePrintText)element;
              if ("Background".equals(jrTemplatePrintText.getKey())) {
                i.remove();
              }
            }
          } else if (element instanceof JRTemplatePrintFrame) {
            jrTemplatePrintFrame = (JRTemplatePrintFrame)element;
            if (jrTemplatePrintFrame.getElements().size() == 0) {
              element = elements.get(1);
              if (element instanceof JRTemplatePrintFrame) {
                jrTemplatePrintFrame = (JRTemplatePrintFrame)element;
                if (jrTemplatePrintFrame.getElements().size() == 0) {
                  i.remove();
                }
              }
            }
          }
      }
    }

  }

  /**
   * Semi-legacy class, imported from {@code com.technology.jep.jepriareport.server.JepReportDataSource} as a quick migration,
   * TODO needs refactoring or rewriting
   */
  protected static class JepReportDataSource implements JRDataSource {
    private static final int DEFAULT_MODE = 0;
    protected JepReportRecords recordList;
    private static final int OUT_OF_MEMORY_TEST_MODE = 1;
    private static final int mode = 0;
    private static final int MAX_ROW_NUMBER_TEST_MODE = 100000;
    private int currentRowNumber;

    public JepReportDataSource(JepReportRecords records) {
      this.recordList = null;
      this.currentRowNumber = 0;
      this.recordList = records;
      this.recordList.beforeFirst();
    }

    public JepReportDataSource(JepReportRecords records, String reportDesignPath) throws JRException {
      this(records);
      JasperDesign jasperDesign = JRXmlLoader.load(reportDesignPath);
      int columnNumber = jasperDesign.getColumnCount();
      if (columnNumber > 1) {
        if (jasperDesign.getPrintOrderValue() == PrintOrderEnum.VERTICAL) {
          records.setPrintOrder(JepReportRecords.PrintOrder.HORIZONTAL);
          jasperDesign.setPrintOrder(PrintOrderEnum.VERTICAL);
          JasperCompileManager.compileReportToFile(jasperDesign, reportDesignPath.replaceAll("jrxml", "jasper"));
        } else {
          records.setPrintOrder(JepReportRecords.PrintOrder.VERTICAL);
        }

        records.setColumnNumber(columnNumber);
      }

    }

    public JepReportDataSource(List<Object> recordList, String reportDesignPath) throws JRException {
      this(new JepReportRecords(recordList), reportDesignPath);
    }

    public boolean next() throws JRException {
      boolean result = false;
      switch(0) {
        case 1:
          if (this.currentRowNumber++ < 100000) {
            this.recordList.first();
            result = true;
            if (this.currentRowNumber % 1000 == 0) {
              System.out.println("Получено " + this.currentRowNumber + " записей");
            }
          }
          break;
        default:
          if (this.recordList.hasNext()) {
            this.recordList.next();
            result = true;
          }
      }

      return result;
    }

    public Object getFieldValue(JRField field) throws JRException {
      String fieldName = field.getName();
      return this.recordList.getCurrentValue(fieldName);
    }

    public boolean beforeFirst() throws JRException {
      this.recordList.beforeFirst();
      return true;
    }
  }

  /**
   * Semi-legacy class, imported from {@code com.technology.jep.jepriareport.server.JepReportRecords} as a quick migration,
   * TODO needs refactoring or rewriting
   */
  protected static class JepReportRecords {
    protected List<?> records;
    private int firstIndex = -1;
    private int lastIndex = -1;
    private int rangeSize = 0;
    private int range = 0;
    private int rowNumber = -1;
    private int currentIndex = -1;
    private int columnNumber = 1;
    private PrintOrder printOrder;
    private int columnSize;
    private final Class<?> dtoClass;

    public JepReportRecords(List<?> recordList) {
      this.printOrder = PrintOrder.VERTICAL;
      this.columnSize = -1;
      this.setRecords(recordList);
      
      if (recordList == null || recordList.isEmpty()) {
        throw new IllegalArgumentException();
      } else {
        this.dtoClass = recordList.iterator().next().getClass();
      }
    }

    public Object getCurrentRecord() {
      return this.records.get(this.currentIndex);
    }

    public void beforeFirst() {
      this.rowNumber = this.currentIndex = this.records.size() > 0 ? this.firstOffset() - 1 : -1;
    }

    public Object first() {
      this.rowNumber = this.currentIndex = this.firstOffset();
      return this.getCurrentRecord();
    }

    public Object last() {
      this.rowNumber = this.currentIndex = this.lastOffset();
      return this.getCurrentRecord();
    }

    public void afterLast() {
      this.rowNumber = this.currentIndex = this.records.size() > 0 ? this.lastOffset() + 1 : -1;
    }

    public Object next() {
      ++this.rowNumber;
      this.setCurrentIndexByRowNumber();
      return this.getCurrentRecord();
    }

    public Object previous() {
      --this.rowNumber;
      this.setCurrentIndexByRowNumber();
      return this.getCurrentRecord();
    }

    public int getRangeSize() {
      return this.rangeSize;
    }

    public void setRangeSize(int rangeSize) {
      this.rangeSize = rangeSize;
    }

    protected int firstOffset() {
      return this.rangeSize > 0 ? this.firstIndex + this.rangeSize * this.range : this.firstIndex;
    }

    protected int lastOffset() {
      return this.rangeSize > 0 && this.rangeSize * this.range + this.rangeSize - 1 < this.lastIndex ? this.rangeSize * this.range + this.rangeSize - 1 : this.lastIndex;
    }

    public boolean hasNext() {
      return this.rowNumber < this.lastOffset();
    }

    public int getCurrentIndex() {
      return this.currentIndex;
    }

    public Object getCurrentValue(String name) {
      String camel = NamingUtil.snake_case2camelCase(name);
      String getterName = "get" + Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
      
      Method getter;
      try {
        getter = dtoClass.getDeclaredMethod(getterName);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
      
      Object value;
      try {
        value = getter.invoke(this.getCurrentRecord());
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
      return value;
    }

    public int getColumnNumber() {
      return this.columnNumber;
    }

    public void setColumnNumber(int columnNumber) {
      this.columnNumber = columnNumber;
    }

    public PrintOrder getPrintOrder() {
      return this.printOrder;
    }

    public void setPrintOrder(PrintOrder printOrder) {
      this.printOrder = printOrder;
    }

    private void setCurrentIndexByRowNumber() {
      if (this.printOrder == PrintOrder.VERTICAL) {
        this.currentIndex = this.rowNumber;
      } else {
        int currentColumn = this.rowNumber % this.columnNumber;
        this.currentIndex = this.rowNumber / this.columnNumber + currentColumn * this.getColumnSize();
      }

    }

    private int getColumnSize() {
      if (this.columnSize == -1) {
        int recordListSize = this.records.size();
        this.columnSize = recordListSize / this.columnNumber + (recordListSize % this.columnNumber == 0 ? 0 : 1);
      }

      return this.columnSize;
    }

    private void setRecords(List<?> newRecords) {
      this.records = newRecords;
      int recordListSize = this.records.size();
      this.firstIndex = recordListSize > 0 ? 0 : -1;
      this.lastIndex = recordListSize > 0 ? recordListSize - 1 : -1;
      this.currentIndex = -1;
    }

    public enum PrintOrder {
      VERTICAL,
      HORIZONTAL;
    }
  }
}
