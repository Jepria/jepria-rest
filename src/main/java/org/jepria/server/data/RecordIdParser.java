package org.jepria.server.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Parses a composite recordId into a map of values
 */
public class RecordIdParser {

  public static final String DELIMITER_DEFAULT = "~"; // '.' and '~' are url-safe, but '~' has lower frequence; ',' and ';' are reserved; space is url-unsafe (see https://www.ietf.org/rfc/rfc3986.txt)

  public static Map<String, String> parseComposite(String recordId) {
    return parseComposite(recordId, DELIMITER_DEFAULT);
  }

  /**
   *
   * @param recordId
   * @param delimiter
   * @return
   * @throws IllegalArgumentException
   */
  public static Map<String, String> parseComposite(String recordId, String delimiter) throws IllegalArgumentException {
    Map<String, String> ret = new HashMap<>();

    if (recordId != null) {
      String[] recordIdParts = recordId.split(delimiter);
      for (String recordIdPart : recordIdParts) {
        if (recordIdPart != null) {
          String[] recordIdPartKv = recordIdPart.split("="); // space is url-
          if (recordIdPartKv.length != 2) {
            throw new IllegalArgumentException("Could not split [" + recordIdPart + "] as a key-value pair with [=] delimiter");
          }
          ret.put(recordIdPartKv[0], recordIdPartKv[1]);
        }
      }
    }

    return ret;
  }

  public static Map<String, Object> parseComposite(String recordId, Function<String, Class<?>> fieldTypeProvider) {
    return parseComposite(recordId, DELIMITER_DEFAULT, fieldTypeProvider);
  }

  public static Map<String, Object> parseComposite(String recordId, String delimiter, Function<String, Class<?>> fieldTypeProvider) {
    Map<String, String> stringMap = parseComposite(recordId);

    Map<String, Object> ret = new HashMap<>();

    // create typed values
    for (final String fieldName: stringMap.keySet()) {
      final String fieldValueStr = stringMap.get(fieldName);

      Class<?> type = fieldTypeProvider.apply(fieldName);
      if (type == null) {
        throw new IllegalArgumentException("Could not determine type for the field '" + fieldName + "'");
      }

      final Object fieldValue = getTypedValue(fieldValueStr, type);

      ret.put(fieldName, fieldValue);
    }

    return ret;
  }

  public static <T> T getTypedValue(String strValue, Class<T> type) {
    if (type == Integer.class) {
      return (T) (Integer) Integer.parseInt(strValue);
    } else if (type == String.class) {
      return (T) strValue;
    } else {
      // TODO add support?
      throw new UnsupportedOperationException("The type '" + type + "' is unsupported for getting typed values");
    }
  }
}


