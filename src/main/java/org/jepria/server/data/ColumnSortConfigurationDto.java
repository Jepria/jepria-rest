package org.jepria.server.data;

/**
 * Dto для представления конфигурации сортировки списка по конкретному столбцу
 */
public class ColumnSortConfigurationDto {
  
  public ColumnSortConfigurationDto() {}

  // TODO this method is Jersey-specific, so move it to the jaxrs's package,
  //  as a ParamConverterProvider implementation
  //  see https://stackoverflow.com/questions/30403033/passing-custom-type-query-parameter
  /**
   * Method used by Jersey for deserializing from a query string param list: sort=field1,asc&sort=field2,desc
   * @param s
   */
  public ColumnSortConfigurationDto(String s) {
    if (s != null) {
      int sepIndex = s.indexOf(',');
      if (sepIndex != -1) {
        setColumnName(s.substring(0, sepIndex));
        setSortOrder(s.substring(sepIndex + 1));
      } else {
        setColumnName(s);
      }
    }
  }
  
  private String columnName;
  
  private String sortOrder;

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public String getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
  }
}

