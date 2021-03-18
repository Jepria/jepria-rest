package org.jepria.server.data;

import java.util.List;

public class SearchResultDto<T> {
  
  private int resultsetSize;
  
  private List<T> data;

  public SearchResultDto() {}
  
  public int getResultsetSize() {
    return resultsetSize;
  }

  public void setResultsetSize(int resultsetSize) {
    this.resultsetSize = resultsetSize;
  }

  public List<T> getData() {
    return data;
  }

  public void setData(List<T> data) {
    this.data = data;
  }
}
