package org.jepria.server.data;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.fill.*;
import net.sf.jasperreports.engine.util.FileBufferedOutputStream;
import net.sf.jasperreports.engine.util.JRSwapFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

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
    JRDataSource jrDataSource = new JRDataSourceImpl(records.iterator());

    if (locale != null) {
      parameters.put("REPORT_LOCALE", locale);
    }

    JRSwapFile swapFile = new JRSwapFile(System.getProperty("java.io.tmpdir"), 1024, 1024);
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

  protected static class JRDataSourceImpl implements JRDataSource {

    protected final Iterator<?> it;

    /**
     * @param recordsIterator nullable, null means empty iterator
     */
    public JRDataSourceImpl(Iterator<?> recordsIterator) {
      if (recordsIterator == null) {
        this.it = Collections.emptyIterator();
      } else {
        this.it = recordsIterator;
      }
    }

    protected Object currentRecord;

    /**
     * Lazy-initialized class which the iterated records are of. 
     */
    protected Class<?> dtoClass;

    @Override
    public boolean next() throws JRException {
      if (it.hasNext()) {
        currentRecord = it.next();

        { // lazy-initialize the dto class
          if (dtoClass == null) {
            dtoClass = currentRecord.getClass();
          }
        }

        return true;
      } else {
        return false;
      }
    }

    @Override
    public Object getFieldValue(JRField jrField) throws JRException {

      String fieldName = jrField.getName();

      String camel = NamingUtil.snake_case2camelCase(fieldName);
      String getterName = "get" + Character.toUpperCase(camel.charAt(0)) + camel.substring(1);

      Method getter;
      try {
        getter = dtoClass.getDeclaredMethod(getterName);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }

      Object value;
      try {
        value = getter.invoke(currentRecord);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
      return value;
    }
  }
}
