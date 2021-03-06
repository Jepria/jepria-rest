package org.jepria.server.data;

import java.util.*;

/**
 * Описание записи
 */
// TODO replace this class with annotations on Dto?
public interface RecordDefinition {
  /**
   * Полный набор имён полей, которые может содержать запись
   * 
   * @return items in snake_case
   */
  // TODO this can be determined from the Dto annotations
  // TODO объяснить в комментарии, какие поля нужно возвращать (все возможные или часть?)
  Set<String> getFieldNames();
  
  /**
   * @return первичный ключ записи (простой или составной), non-null, items in snake_case
   */
  // TODO this can be determined from the Dto annotations
  List<String> getPrimaryKey();

  /**
   * Определяет тип поля записи
   * @param field_name in snake_case
   * @return
   */
  // TODO Class or Type?
  // TODO this can be determined from the Dto annotations
  Class<?> getFieldType(String field_name);

  /**
   * Определяет способ сравнения значений определённого поля записи
   * @param field_name in snake_case
   * @return custom field comparator or {@code null} to use default comparator
   */
  default Comparator<Object> getFieldComparator(String field_name) {
    return null;
  }
  
  class IncompletePrimaryKeyException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public IncompletePrimaryKeyException(String message) {
      super(message);
    }
  }
  
  /**
   * Извлекает из данной записи первичный ключ, корректируя регистр полей в соответствии с объявлением в {@link #getPrimaryKey()}
   * @param record запись, non-null
   * @return первичный ключ, non-null
   * @throws IncompletePrimaryKeyException если входная запись содержит не все поля из первичного ключа
   * @throws IllegalArgumentException если входная запись есть null
   */
  default <X> Map<String, X> buildPrimaryKey(Map<String, X> record) throws IncompletePrimaryKeyException {
    if (record == null) {
      throw new IllegalArgumentException("record is null");
    }

    final List<String> primaryKey = getPrimaryKey();
    
    final Map<String, X> ret = new HashMap<>();

    for (Map.Entry<String, X> e: record.entrySet()) {
      // Note: проверка полей регистронезависимым методом
      for (String primaryKeyElement: primaryKey) {
        if (primaryKeyElement.equalsIgnoreCase(e.getKey())) {
          ret.put(primaryKeyElement, e.getValue()); // associate original key with a value from the record
          break;
        }
      }
    }

    // Проверяем, что все поля первичного ключа присутствуют в записи (здесь осталось достаточным проверить размер)
    if (ret.size() != primaryKey.size()) {
      throw new IncompletePrimaryKeyException("primary key: " + primaryKey + ", record: " + record);
    }

    return ret;
  }
  
  
  /**
   * Parse recordId (simple or composite) into a primary key map with typed values
   * @param recordId
   * @return
   * @throws RecordDefinition.IncompletePrimaryKeyException
   */
  default Map<String, Object> parseRecordId(String recordId) throws IncompletePrimaryKeyException {
    final String delimiter = "~"; // '.' and '~' are url-safe, but '~' has lower frequence; ',' and ';' are reserved; space is url-unsafe (see https://www.ietf.org/rfc/rfc3986.txt)
    return parseRecordId(recordId, delimiter);
  }
  
  /**
   * Parse recordId (simple or composite) into a primary key map with typed values
   * @param recordId
   * @param delimiter between key=value pairs
   * @return
   * @throws RecordDefinition.IncompletePrimaryKeyException
   * @throws IllegalArgumentException if 'key=value' pair syntax of the input is wrong
   */
  default Map<String, Object> parseRecordId(String recordId, String delimiter) throws IncompletePrimaryKeyException, IllegalArgumentException {
    Map<String, Object> ret = new HashMap<>();
  
    if (recordId != null) {
      final List<String> primaryKey = getPrimaryKey();
    
      if (primaryKey.size() == 1) {
        // simple primary key: "value"
      
        final String fieldName = primaryKey.get(0);

        Class<?> type = getFieldType(fieldName);
        if (type == null) {
          throw new IllegalArgumentException("Could not determine type for the field '" + fieldName + "'");
        }

        final Object fieldValue = RecordIdParser.getTypedValue(recordId, type);
      
        ret.put(fieldName, fieldValue);
      
      } else if (primaryKey.size() > 1) {
        // composite primary key: "key1=value1.key2=value2" or "key1=value1~key2=value2"

        ret = RecordIdParser.parseComposite(recordId, delimiter, this::getFieldType);
      }
    }
  
    return ret;
  }
}
