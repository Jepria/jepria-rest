package org.jepria.server.data;

import java.io.PrintWriter;

/**
 * Creates and prints .xls file from headers and rows data 
 */
public class ExcelDao {
  
  public static final String XLS_XML_HEADER =
          "<?xml version=\"1.0\"?>\r\n" +
                  "<?mso-application progid=\"Excel.Sheet\"?>\r\n" +
                  "<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\r\n" +
                  "  xmlns:o=\"urn:schemas-microsoft-com:office:office\"\r\n" +
                  "  xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\r\n" +
                  "  xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\r\n" +
                  "  xmlns:html=\"http://www.w3.org/TR/REC-html40\">\r\n" +
                  "  <Styles>\r\n" +
                  "    <Style ss:ID=\"Default\">\r\n" +
                  "      <Alignment ss:Vertical=\"Top\" ss:Horizontal=\"Left\" />\r\n" +
                  "    </Style>\r\n" +
                  "    <Style ss:ID=\"Header\">\r\n" +
                  "      <Alignment ss:Vertical=\"Top\" ss:Horizontal=\"Center\" ss:WrapText=\"1\"/>\r\n" +
                  "      <Font ss:Bold=\"1\"/>\r\n" +
                  "    </Style>\r\n" +
                  "  </Styles>\r\n" +
                  "  <Worksheet ss:Name=\"ExcelReport\">" +
                  "  	<Table>";

  public static final String XLS_XML_FOOTER =
          "   </Table>\r\n" +
                  " <WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">\r\n" +
                  "   <FrozenNoSplit/>\r\n" +
                  "   <SplitHorizontal>1</SplitHorizontal>\r\n" +
                  "   <TopRowBottomPane>1</TopRowBottomPane>\r\n" +
                  "   <ActivePane>2</ActivePane>\n" +
                  " </WorksheetOptions>\r\n" +
                  "  </Worksheet>\r\n"+
                  "</Workbook>\r\n";

  public static final String XLS_CELL_PREFIX =
          "      <Column ss:Width=\"150\"/>";

  public static final String XLS_XML_HEADER_CELL_PREFIX =
          "        <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">";

  public static final String XLS_XML_STRING_CELL_PREFIX =
          "        <Cell ss:StyleID=\"Default\"><Data ss:Type=\"String\">";

  public static final String XLS_XML_CELL_POSTFIX =
          "</Data></Cell>";

  public static final String XLS_XML_ROW_PREFIX =
          "      <Row>";

  public static final String XLS_XML_ROW_POSTFIX =
          "      </Row>";

  /**
   * 
   * @param headers header titles
   * @param rows pre-formatted cell values to print as-is
   * @param out writed to print the output excel
   */
  public static void print(String[] headers, String[][] rows, PrintWriter out) {

    out.println(XLS_XML_HEADER);

    StringBuilder columns = new StringBuilder(headers.length);
    StringBuilder cells = new StringBuilder(headers.length);
    for (String header: headers){
      columns.append(XLS_CELL_PREFIX);
      cells.append(XLS_XML_HEADER_CELL_PREFIX + header + XLS_XML_CELL_POSTFIX);
    }

    out.println(columns.toString() +
            XLS_XML_ROW_PREFIX +
            cells.toString() +
            XLS_XML_ROW_POSTFIX);

    if (rows != null) {
      for (String[] rowData: rows) {
        StringBuilder row = new StringBuilder();
        for (String field: rowData){
          row.append(XLS_XML_STRING_CELL_PREFIX + field + XLS_XML_CELL_POSTFIX);
        }
        out.println(XLS_XML_ROW_PREFIX +
                row.toString() +
                XLS_XML_ROW_POSTFIX);
      }
    }

    out.println(XLS_XML_FOOTER);

    out.flush();
  }
}
