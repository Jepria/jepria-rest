package org.jepria.server.service.rest;

import org.jepria.server.data.ColumnSortConfigurationDto;
import org.jepria.server.data.SearchResultDto;
import org.jepria.server.service.security.JepSecurityContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8") // might be overridden on an application method level
@Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8") // might be overridden on an application method level
@Path("") // important
/**
 * jaxrs-адаптер (транспортный слой)
 * <br/>
 * <i>В устаревшей терминологии: endpoint, EndpointBase</i>
 * <br/>
 */
public class JaxrsAdapterBase {

  protected JaxrsAdapterBase() {}

  @Context
  protected HttpServletRequest request;

  @Context
  protected JepSecurityContext securityContext;

  /**
   * Adapter for endpoint methods related to entity operations
   */
  public class EntityEndpointAdapter {

    protected final Supplier<EntityService> entityService;

    public EntityEndpointAdapter(Supplier<EntityService> entityService) {
      this.entityService = entityService;
    }

    public Object getRecordById(String recordId) {
      final Object record;

      try {
        record = entityService.get().getRecordById(recordId, securityContext.getCredential());
      } catch (NoSuchElementException e) {
        // 404
        throw new NotFoundException(e);
      }

      return record;
    }

    public Response create(Object record) {
      final String createdId = entityService.get().create(record, securityContext.getCredential());

      // ссылка на созданную запись
      final URI location = URI.create(request.getRequestURL() + "/" + createdId);
      Response response = Response.created(location).build();

      return response;
    }

    public void deleteRecordById(String recordId) {
      entityService.get().deleteRecord(recordId, securityContext.getCredential());
    }

    public void update(String recordId, Object record) {
      entityService.get().update(recordId, record, securityContext.getCredential());
    }

  }

  /**
   * Adapter for endpoint methods related to search operations
   */
  public class SearchEndpointAdapter {

    protected final Supplier<SearchService> searchService;

    public SearchEndpointAdapter(Supplier<SearchService> searchService) {
      this.searchService = searchService;
    }

    /**
     * @param pageSize          may be null
     * @param page              may be null
     * @param sortConfiguration
     * @param templateDto
     * @param cacheControl
     * @return
     */
    public <T> SearchResultDto<T> search(Integer pageSize,
                                  Integer page,
                                  List<ColumnSortConfigurationDto> sortConfiguration,
                                  Object templateDto,
                                  String cacheControl) {

      // Convert Dtos (for transferring) to a SearchRequest (internal representation)
      final SearchService.SearchRequest searchRequest;
      {
        final Map<String, Integer> sortConfig = convertListSortConfig(sortConfiguration);
        searchRequest = new SearchService.SearchRequest(templateDto, sortConfig);
      }

      final SearchService.SearchResult result;

      if (pageSize != null && page != null) {
        result = searchService.get().search(pageSize, page, searchRequest, cacheControl, securityContext.getCredential());

      } else if (pageSize == null && page == null) {
        result = searchService.get().search(searchRequest, cacheControl, securityContext.getCredential());

      } else {
        final String message = "Either 'pageSize' and 'page' query params are both empty (for getting whole resultset), "
                + "or both non-empty (for getting resultset paged)";
        throw new BadRequestException(message);
      }
      
      SearchResultDto<T> searchResultDto = new SearchResultDto<>();
      searchResultDto.setResultsetSize(result.resultsetSize);
      searchResultDto.setData((List<T>)result.data);

      return searchResultDto;
    }

    /**
     * @param listSortConfig
     * @return <b>ordered</b> map, modifiable collection, null for null is important
     */
    protected Map<String, Integer> convertListSortConfig(List<ColumnSortConfigurationDto> listSortConfig) {
      if (listSortConfig == null) {
        return null;
      }

      final LinkedHashMap<String, Integer> ret = new LinkedHashMap<>();
      for (ColumnSortConfigurationDto colSortConfig : listSortConfig) {
        ret.put(colSortConfig.getColumnName(), "desc".equals(colSortConfig.getSortOrder()) ? -1 : 1);
      }
      return ret;
    }
  }
}
