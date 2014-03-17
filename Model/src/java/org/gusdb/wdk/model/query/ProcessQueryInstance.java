package org.gusdb.wdk.model.query;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;
import javax.xml.rpc.ServiceException;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.ArrayUtil;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.db.platform.DBPlatform;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunner.ArgumentBatch;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.dbms.ArrayResultList;
import org.gusdb.wdk.model.dbms.CacheFactory;
import org.gusdb.wdk.model.dbms.ResultList;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wsf.client.WsfServiceServiceLocator;
import org.gusdb.wsf.service.WsfRequest;
import org.gusdb.wsf.service.WsfServiceException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This process query instance calls the web service, retrieves the result, and
 * cache them into the cache table. If the result generated by the service is
 * too big, the service will send it in multiple packets, and the process query
 * instance will retrieve all the packets in order.
 * 
 * @author Jerric Gao
 */
public class ProcessQueryInstance extends QueryInstance {

  private static final Logger logger = Logger.getLogger(ProcessQueryInstance.class);

  private ProcessQuery query;
  private int signal;

  public ProcessQueryInstance(User user, ProcessQuery query,
      Map<String, String> values, boolean validate, int assignedWeight,
      Map<String, String> context) throws WdkModelException, WdkUserException {
    super(user, query, values, validate, assignedWeight, context);
    this.query = query;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.gusdb.wdk.model.query.QueryInstance#appendSJONContent(org.json.JSONObject
   * )
   */
  @Override
  protected void appendSJONContent(JSONObject jsInstance) throws JSONException {
    jsInstance.put("signal", signal);
  }

  // Have to turn this off, since it is not easy to add extra column into the 
  // result with the ArgumentBatch.
  private static final boolean USE_SQLRUNNER = false;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.gusdb.wdk.model.query.QueryInstance#insertToCache(java.sql.Connection ,
   * java.lang.String)
   */
  @Override
  public void insertToCache(String tableName, int instanceId)
      throws WdkModelException {

    logger.debug("inserting process query result to cache...");
    List<Column> columns = Arrays.asList(query.getColumns());
    Set<String> columnNames = query.getColumnMap().keySet();

    // prepare the sql
    String sql = buildCacheInsertSql(tableName, instanceId, columns,
        columnNames);

    // get results and time process
    long startTime = System.currentTimeMillis();
    final ResultList resultList = getUncachedResults();
    logger.info("Getting uncached results took "
        + ((System.currentTimeMillis() - startTime) / 1000D) + " seconds");

    // start timer again for insertion to cache
    startTime = System.currentTimeMillis();
    DataSource dataSource = wdkModel.getAppDb().getDataSource();

    // NOTE: This code may be used in the future instead of the code below;
    // probably
    // want to really test results to make sure exact same data gets inserted
    if (USE_SQLRUNNER) {
      ArgumentBatch dataStream = new ResultListArgumentBatch(resultList,
          columns, query.getCacheInsertBatchSize());
      SQLRunner runner = new SQLRunner(dataSource, sql);
      runner.executeStatementBatch(dataStream);
      long cumulativeBatchTime = runner.getLastExecutionTime();
      long cumulativeInsertTime = System.currentTimeMillis() - startTime;
      logger.info("All batches completed.\nInserting results to cache took "
          + (cumulativeInsertTime / 1000D)
          + " seconds (Java + Oracle clock time)\n"
          + (cumulativeBatchTime / 1000D)
          + " seconds of that were spent executing batches ("
          + FormatUtil.getPctFromRatio(cumulativeBatchTime,
              cumulativeInsertTime) + ")");
      logger.debug("Process query cache insertion finished.");
      return;
    }

    // get bind types for each column
    Integer[] bindTypes = ResultListArgumentBatch.getBindTypes(columns);
    
    // add rowID into bindTypes
    bindTypes = ArrayUtil.insert(bindTypes, 0, Types.INTEGER);
    
    PreparedStatement ps = null;
    int rowCount = 0;
    try {
      ps = SqlUtils.getPreparedStatement(dataSource, sql.toString());
      int rowsInBatch = 0, numBatches = 0;
      long cumulativeBatchTime = 0;
      while (resultList.next()) {
        
        // build typed object array from this record, bind to statement, and add
        // to batch
        Object[] values = ResultListArgumentBatch.getNextRecordValues(columns,
            resultList);
        
        // add rowCount into values
        rowCount++;
        values = ArrayUtil.insert(values, 0, rowCount);

        SqlUtils.bindParamValues(ps, bindTypes, values);
        ps.addBatch();

        // if reached the batch size, send to DB
        rowsInBatch++;
        if (rowsInBatch == query.getCacheInsertBatchSize()) {
          numBatches++;
          cumulativeBatchTime = executeBatchWithLogging(ps, numBatches,
              rowsInBatch, cumulativeBatchTime);
          rowsInBatch = 0;
        }
      }
      // send any remainder to DB
      if (rowsInBatch > 0) {
        numBatches++;
        cumulativeBatchTime = executeBatchWithLogging(ps, numBatches,
            rowsInBatch, cumulativeBatchTime);
      }

      long cumulativeInsertTime = System.currentTimeMillis() - startTime;
      logger.info("All batches completed.\nInserting results to cache took "
          + (cumulativeInsertTime / 1000D)
          + " seconds (Java + Oracle clock time)\n"
          + (cumulativeBatchTime / 1000D)
          + " seconds of that were spent executing batches ("
          + FormatUtil.getPctFromRatio(cumulativeBatchTime,
              cumulativeInsertTime) + ")");
    } catch (SQLException e) {
      throw new WdkModelException("Unable to insert record into cache.", e);
    } finally {
      SqlUtils.closeStatement(ps);
    }
    logger.debug("Process query cache insertion finished.");
  }

  private long executeBatchWithLogging(PreparedStatement ps, int numBatches,
      int rowsInBatch, long cumulativeBatchTime) throws SQLException {
    long batchStart = System.currentTimeMillis();
    ps.executeBatch();
    long batchElapsed = System.currentTimeMillis() - batchStart;
    cumulativeBatchTime += batchElapsed;
    logger.debug("Writing batch " + numBatches + " (" + rowsInBatch
        + " records) took " + batchElapsed + " ms. Cumulative batch "
        + "execution time: " + cumulativeBatchTime + " ms");
    return cumulativeBatchTime;
  }

  private String buildCacheInsertSql(String tableName, int instanceId,
      List<Column> columns, Set<String> columnNames) {

    String weightColumn = Utilities.COLUMN_WEIGHT;

    StringBuilder sql = new StringBuilder("INSERT INTO " + tableName);
    sql.append(" (").append(CacheFactory.COLUMN_INSTANCE_ID + ", ");
    sql.append(CacheFactory.COLUMN_ROW_ID);

    // have to move clobs to the end of bind variables or Oracle will complain
    for (Column column : columns) {
      if (column.getType() != ColumnType.CLOB)
        sql.append(", ").append(column.getName());
    }
    for (Column column : columns) {
      if (column.getType() == ColumnType.CLOB)
        sql.append(", ").append(column.getName());
    }

    // insert name of weight as the last column, if it doesn't exist
    if (query.isHasWeight() && !columnNames.contains(weightColumn))
      sql.append(", ").append(weightColumn);

    sql.append(") VALUES (");
    sql.append(instanceId).append(", ?");
    for (int i = 0; i < columns.size(); i++) {
      sql.append(", ?");
    }

    // insert weight to the last column, if doesn't exist
    if (query.isHasWeight() && !columnNames.contains(weightColumn))
      sql.append(", ").append(assignedWeight);

    return sql.append(")").toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.query.QueryInstance#getUncachedResults(org.gusdb.
   * wdk.model.Column[], java.lang.Integer, java.lang.Integer)
   */
  @Override
  protected ResultList getUncachedResults() throws WdkModelException {
    WsfRequest request = new WsfRequest();
    request.setPluginClass(query.getProcessName());
    request.setProjectId(wdkModel.getProjectId());

    // prepare parameters
    Map<String, String> paramValues = getParamInternalValues();
    HashMap<String, String> params = new HashMap<String, String>();
    for (String name : paramValues.keySet()) {
      params.put(name, paramValues.get(name));
    }
    request.setParams(params);

    // prepare columns
    Map<String, Column> columns = query.getColumnMap();
    String[] columnNames = new String[columns.size()];
    Map<String, Integer> indices = new LinkedHashMap<String, Integer>();
    columns.keySet().toArray(columnNames);
    String temp = "";
    for (int i = 0; i < columnNames.length; i++) {
      // if the wsName is defined, reassign it to the columns
      Column column = columns.get(columnNames[i]);
      if (column.getWsName() != null) columnNames[i] = column.getWsName();
      indices.put(column.getName(), i);
      temp += columnNames[i] + ", ";
    }
    request.setOrderedColumns(columnNames);
    logger.debug("process query columns: " + temp);

    request.setContext(context);

    StringBuffer resultMessage = new StringBuffer();
    try {
      ProcessResponse response = getResponse(request, query.isLocal());
      this.resultMessage = response.getMessage();
      this.signal = response.getSignal();
      String[][] content = response.getResult();

      logger.debug("WSQI Result Message:" + resultMessage);
      logger.info("Result Array size = " + content.length);

      // add weight if needed
      String weightColumn = Utilities.COLUMN_WEIGHT;
      if (query.isHasWeight() && !columns.containsKey(weightColumn)) {
        indices.put(weightColumn, indices.size());
        for (int i = 0; i < content.length; i++) {
          content[i] = ArrayUtil.append(content[i], Integer.toString(assignedWeight));
        }
      }

      ArrayResultList result = new ArrayResultList(response, indices);
      result.setHasWeight(query.isHasWeight());
      result.setAssignedWeight(assignedWeight);
      return result;
    } catch (RemoteException | MalformedURLException | ServiceException
        | WsfServiceException ex) {
      throw new WdkModelException(ex);
    }
  }

  private ProcessResponse getResponse(WsfRequest request, boolean local)
      throws RemoteException, MalformedURLException, ServiceException,
      WsfServiceException {

    long start = System.currentTimeMillis();
    String jsonRequest = request.toString();
    ProcessResponse response;

    if (local) { // invoke the process query locally
      logger.info("Using local service");
      // call the service directly
      org.gusdb.wsf.service.WsfService service = new org.gusdb.wsf.service.WsfService();
      org.gusdb.wsf.service.WsfResponse wsfResponse = service.invoke(jsonRequest);
      response = new ServiceProcessResponse(service, wsfResponse);
    }

    else { // invoke the process query via web service
      logger.info("Using remote service");
      // call the service through client
      String serviceUrl = query.getWebServiceUrl();
      logger.info("Invoking " + request.getPluginClass() + " at " + serviceUrl);
      WsfServiceServiceLocator locator = new WsfServiceServiceLocator();
      org.gusdb.wsf.client.WsfService client = locator.getWsfService(new URL(
          serviceUrl));
      org.gusdb.wsf.client.WsfResponse wsfResponse = client.invoke(jsonRequest);
      response = new ClientProcessResponse(client, wsfResponse);
    }
    long end = System.currentTimeMillis();
    logger.debug("Client took " + ((end - start) / 1000.0) + " seconds.");

    return response;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.query.QueryInstance#getSql()
   */
  @Override
  public String getSql() throws WdkModelException {
    // always get sql that queries on the cached result
    return getCachedSql();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.gusdb.wdk.model.query.QueryInstance#createCache(java.sql.Connection,
   * java.lang.String, int)
   */
  @Override
  public void createCache(String tableName, int instanceId)
      throws WdkModelException {
    logger.debug("creating process query cache...");
    DBPlatform platform = query.getWdkModel().getAppDb().getPlatform();
    Column[] columns = query.getColumns();

    StringBuffer sqlTable = new StringBuffer("CREATE TABLE " + tableName + " (");

    // define the instance id column
    String numberType = platform.getNumberDataType(12);
    sqlTable.append(CacheFactory.COLUMN_INSTANCE_ID + " " + numberType + " NOT NULL, ");
    sqlTable.append(CacheFactory.COLUMN_ROW_ID + " " + numberType + " NOT NULL");
    if (query.isHasWeight())
      sqlTable.append(", " + Utilities.COLUMN_WEIGHT + " " + numberType);

    // define the rest of the columns
    for (Column column : columns) {
      // weight column is already added to the sql.
      if (column.getName().equals(Utilities.COLUMN_WEIGHT)
          && query.isHasWeight()) continue;

      int width = column.getWidth();
      ColumnType type = column.getType();

      String strType;
      if (type == ColumnType.BOOLEAN) {
        strType = platform.getBooleanDataType();
      } else if (type == ColumnType.CLOB) {
        strType = platform.getClobDataType();
      } else if (type == ColumnType.DATE) {
        strType = platform.getDateDataType();
      } else if (type == ColumnType.FLOAT) {
        strType = platform.getFloatDataType(width);
      } else if (type == ColumnType.NUMBER) {
        strType = platform.getNumberDataType(width);
      } else if (type == ColumnType.STRING) {
        strType = platform.getStringDataType(width);
      } else {
        throw new WdkModelException("Unknown data type [" + type
            + "] of column [" + column.getName() + "]");
      }

      sqlTable.append(", " + column.getName() + " " + strType);
    }
    sqlTable.append(")");

    try {
      DataSource dataSource = wdkModel.getAppDb().getDataSource();
      SqlUtils.executeUpdate(dataSource, sqlTable.toString(),
          query.getFullName() + "__create-cache-table");
    } catch (SQLException e) {
      throw new WdkModelException("Unable to create cache table.", e);
    }
    // also insert the result into the cache
    insertToCache(tableName, instanceId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.query.QueryInstance#getResultSize()
   */
  @Override
  public int getResultSize() throws WdkModelException {
    if (!isCached()) {
      int count = 0;
      ResultList resultList = getResults();
      while (resultList.next()) {
        count++;
      }
      return count;
    } else return super.getResultSize();
  }

}
