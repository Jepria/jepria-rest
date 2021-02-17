package org.jepria.server.service.rest;

import com.google.gson.Gson;
import org.jepria.server.service.security.Credential;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Сервис поиска объектов сущности
 */
public interface SearchService {

  /**
   * Объект (совокупность его полей) однозначно идентифицирует поиск.
   * Интерфейс клиентского поискового запроса для использования внутри сервиса (internal representation)
   */
  class SearchRequest implements Serializable { // TODO why Serializable?

    /**
     * оригинальный поисковый шаблон
     */
    public final Object templateDto;
    /**
     * @return <b>упорядоченный</b> map.
     * <br/>
     * ключ: имя столбца списка для сортировки записей по нему,
     * <br/>
     * значение: порядок сортировки записей списка по данному столбцу (неотрицательное: по возрастанию, отрицательное: по убыванию)
     * <br/>
     */
    public final Map<String, Integer> listSortConfig;

    private final String templateToken;
    /**
     * @return поисковый шаблон в виде строкового токена, используемый для сравнения двух шаблонов на equals (сравнение оригинальных объектов может быть ненадёжным)
     */
    public String getTemplateToken() {
      return templateToken;
    }
    
    public SearchRequest(Object templateDto, Map<String, Integer> listSortConfig) {
      this.templateDto = templateDto;
      this.listSortConfig = listSortConfig;

      // для преобразования в токен используется не общий контексно-зависимый сериализатор, а просто _некий_ сериализатор
      this.templateToken = templateDto == null ? null : new Gson().toJson(templateDto);
    }
  }
  
  class SearchResult implements Serializable { // TODO why Serializable?
    public final int resultsetSize; 
    public List<?> data;

    public SearchResult(int resultsetSize, List<?> data) {
      this.resultsetSize = resultsetSize;
      this.data = data;
    }
  }
  
  /**
   * 
   * @param searchRequest
   * @param cacheControl
   * @param credential
   * @return
   */
  SearchResult search(SearchRequest searchRequest, String cacheControl, Credential credential);

  /**
   *
   * @param pageSize
   * @param page
   * @param searchRequest
   * @param cacheControl
   * @param credential
   * @return
   */
  SearchResult search(int pageSize, int page, SearchRequest searchRequest, String cacheControl, Credential credential);
}
