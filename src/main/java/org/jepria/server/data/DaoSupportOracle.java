package org.jepria.server.data;

import org.jepria.compat.server.dao.ResultSetMapper;
import org.jepria.compat.server.download.blob.BinaryFileDownloadImpl;
import org.jepria.compat.server.download.blob.FileDownloadStream;
import org.jepria.compat.server.download.clob.FileDownloadReader;
import org.jepria.compat.server.download.clob.TextFileDownloadImpl;
import org.jepria.compat.server.upload.blob.BinaryFileUploadImpl;
import org.jepria.compat.server.upload.blob.FileUploadStream;
import org.jepria.compat.server.upload.clob.FileUploadWriter;
import org.jepria.compat.server.upload.clob.TextFileUploadImpl;
import org.jepria.compat.shared.exceptions.ApplicationException;

import java.io.*;
import java.sql.SQLException;
import java.util.List;

public class DaoSupportOracle implements DaoSupport {
  @Override
  public <T> T create(String query, Class<? super T> resultTypeClass, Object... params) {
    // TODO this is backward compat, independent implementation required
    try {
      return org.jepria.compat.server.dao.DaoSupport.create(query, resultTypeClass, params);
    } catch (ApplicationException e) {
      if (e.getCause() instanceof SQLException) {
        throw new RuntimeSQLException((SQLException) e.getCause());
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void execute(String query, Object... params) {
    // TODO this is backward compat, independent implementation required
    try {
      org.jepria.compat.server.dao.DaoSupport.execute(query, params);
    } catch (ApplicationException e) {
      if (e.getCause() instanceof SQLException) {
        throw new RuntimeSQLException((SQLException) e.getCause());
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public <T> List<T> find(String query, ResultSetMapper<? super T> mapper, Class<? super T> dtoClass, Object... params) {
    // TODO this is backward compat, independent implementation required
    try {
      return org.jepria.compat.server.dao.DaoSupport.find(query, mapper, dtoClass, params);
    } catch (ApplicationException e) {
      if (e.getCause() instanceof SQLException) {
        throw new RuntimeSQLException((SQLException) e.getCause());
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public <T> T executeAndReturn(String query, Class<? super T> resultTypeClass, Object... params) {
    // TODO this is backward compat, independent implementation required
    try {
      return org.jepria.compat.server.dao.DaoSupport.executeAndReturn(query, resultTypeClass, params);
    } catch (ApplicationException e) {
      if (e.getCause() instanceof SQLException) {
        throw new RuntimeSQLException((SQLException) e.getCause());
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public <T> List<T> select(String query, ResultSetMapper<? super T> mapper, Class<? super T> dtoClass, Object... params) {
    // TODO this is backward compat, independent implementation required
    try {
      return org.jepria.compat.server.dao.DaoSupport.select(query, mapper, dtoClass, params);
    } catch (ApplicationException e) {
      if (e.getCause() instanceof SQLException) {
        throw new RuntimeSQLException((SQLException) e.getCause());
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void update(String query, Object... params) {
    // TODO this is backward compat, independent implementation required
    try {
      org.jepria.compat.server.dao.DaoSupport.update(query, params);
    } catch (ApplicationException e) {
      if (e.getCause() instanceof SQLException) {
        throw new RuntimeSQLException((SQLException) e.getCause());
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void delete(String query, Object... params) {
    // TODO this is backward compat, independent implementation required
    try {
      org.jepria.compat.server.dao.DaoSupport.delete(query, params);
    } catch (ApplicationException e) {
      if (e.getCause() instanceof SQLException) {
        throw new RuntimeSQLException((SQLException) e.getCause());
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void deleteClob(String tableName, String dataFieldName, String whereClause) {
    // TODO stub implementation

    final Reader emptyReader = new Reader() {
      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        return -1;
      }
      @Override
      public void close() throws IOException {
      }
    };

    uploadClob(tableName, dataFieldName, whereClause, emptyReader);
  }

  @Override
  public void uploadClob(String tableName, String dataFieldName, String whereClause, Reader reader) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileUploadWriter.uploadFile(
              reader
              , new TextFileUploadImpl()
              , tableName
              , dataFieldName
              , whereClause + " and " + 1, 1 // internally transformed to "where [whereClause] and 1=1"
              , null
              , null
              , false
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteBlob(String tableName, String dataFieldName, String whereClause) {
    // TODO stub implementation

    final InputStream emptyStream = new InputStream() {
      @Override
      public int read() throws IOException {
        return -1;
      }
    };

    uploadBlob(tableName, dataFieldName, whereClause, emptyStream);
  }

  @Override
  public void uploadBlob(String tableName, String dataFieldName, String whereClause, InputStream stream) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileUploadStream.uploadFile(
              stream
              , new BinaryFileUploadImpl() // transaction logic is performed by org.jepria.compat.server.dao.transaction.TransactionFactory.TransactionInvocationHandler Dao wrapper
              , tableName
              , dataFieldName
              , whereClause + " and " + 1, 1 // internally transformed to "where [whereClause] and 1=1"
              , null
              , null
              , false
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void downloadClob(String tableName, String dataFieldName, String whereClause, Writer writer) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileDownloadReader.downloadFile(
              writer
              , new TextFileDownloadImpl()
              , tableName
              , dataFieldName
              , " ( " + whereClause + " ) and " + 1, 1 // internally transformed to "where ( [whereClause] ) and 1=1"
              , null
              , null
              , false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void downloadBlob(String tableName, String dataFieldName, String whereClause, OutputStream stream) {
    // TODO stub implementation from org.jepria.compat.server.upload.JepUploadServlet
    try {
      FileDownloadStream.downloadFile(
              stream
              , new BinaryFileDownloadImpl()
              , tableName
              , dataFieldName
              , " ( " + whereClause + " ) and " + 1, 1 // internally transformed to "where ( [whereClause] ) and 1=1"
              , null
              , null
              , false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}