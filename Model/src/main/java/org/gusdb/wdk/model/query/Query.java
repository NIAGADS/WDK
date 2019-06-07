package org.gusdb.wdk.model.query;

import org.gusdb.fgputil.EncryptionUtil;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.query.param.AnswerParam;
import org.gusdb.wdk.model.query.param.Param;
import org.gusdb.wdk.model.query.param.ParamValuesSet;
import org.gusdb.wdk.model.query.param.ParameterContainerImpl;
import org.gusdb.wdk.model.query.spec.QueryInstanceSpec;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.attribute.AttributeFieldDataType;
import org.gusdb.wdk.model.test.sanity.OptionallyTestable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The query in WDK defines how the data is accessed from the resource. There
 * are currently two kinds of query, SQL based query, and web service based
 * query. The query is not exposed to the user, only the question are visible on
 * the web sites as searches.
 *
 * <p>
 * A Query holds only the definition of query, such as params, SQL template, or
 * information about the web service etc. It can be used to create
 * QueryInstance, which will hold param values, and does the real work of
 * executing a query and retrieve data.
 *
 * <p>
 * Depending on how many answerParams a query might have, a query can be called
 * as a normal query (without any answerParam), or a combined query (with one or
 * more answerParams). If a query has exactly one answerParam, it is also called
 * a transform query; in the transform query, the type of the answerParam can be
 * different from the type of the results the query returns. And there is
 * another special kind of combined query, called BooleanQuery, which has
 * exactly two answerParams, and the types of the answerParam are the same as
 * the result of the query.
 *
 * <p>
 * A query can be used in four contexts in WDK, as ID query, attribute query,
 * table query, and param query. and SqlQuery can be used in all four contexts,
 * but ProcessQuery (web service query) can only be used in ID and param
 * queries.
 *
 * <p>
 * An ID query is a query referenced by a question, and the parameters for the
 * search (the visual name of the question) are defined in the queries. An ID
 * query should return all the primary key columns of the recordClass type the
 * associated question linked to. The the primary key values returned by ID
 * query should be unique, and cannot have duplicate rows. If duplicate primary
 * key occurs, WDK will fail when joining it with the attribute query. An ID
 * query can have other columns other than the primary key columns, and those
 * columns are usually used for the dynamic attributes.
 *
 * <p>
 * An attribute query is a query referenced by a recordClass, in the
 * <attributeQueryRef> tag. An attribute query has to be SqlQuery, and it does
 * not normally have params, although you can define an internal wdk user param
 * in some rare case where the content of the result is user-dependent. The
 * attribute query should return all possible records of a given record type,
 * and the records in the result has to be unique, and the attribute query has
 * to return all the primary key columns, although the corresponding
 * ColumnAttributeField is optional for those columns. The attribute query will
 * be used in two contexts, for single records, and for records in an answer.
 * When used in single record, the attribute SQL is wrapped with the primary key
 * values to return only one row for the record. When used in answer, the
 * attribute SQL is used for sorting the result on the columns in the attribute
 * query, and then the paged id SQL will be used to join with the attribute SQL,
 * to return a page of attributes for the records.
 *
 * <p>
 * An table query is query referenced by recordClass, in the &lt;table&gt; tag.
 * A table query has to be SqlQuery, and it doesn't normally have params,
 * although you can define an internal wdk user param same way as in attribute
 * query. The table query should return the results for all possible records of
 * a given record type, and each record can have zero or more rows in the
 * result. The table query also must return all the primary key columns,
 * although the ColumnAttributeField of those is optional. The table can be used
 * in two contexts, in single record, or in an answer. In a single record, the
 * table query is used in the similar way as attribute query, and it will be
 * wrapped with the primary key values of the record, to get zero or more rows.
 * In the context of an answer, the table SQL can be used to be combined with
 * the paged ID SQL to get a page of the results for the records.
 *
 * @author Jerric Gao
 */
public abstract class Query extends ParameterContainerImpl implements OptionallyTestable {

  public static QueryInstance<?> makeQueryInstance(RunnableObj<QueryInstanceSpec> validSpec)
      throws WdkModelException {
    // unwrap the spec and use to create an instance of the proper type
    QueryInstanceSpec spec = validSpec.get();
    return spec.getQuery().makeInstance(validSpec);
  }

  private String name;

  // temp list, will be discarded after resolve references
  private List<Column> columnList;
  protected Map<String, Column> columnMap;

  // for sanity testing
  private boolean doNotTest;
  private List<ParamValuesSet> paramValuesSets;

  private QuerySet querySet;

  private String[] indexColumns;

  private boolean hasWeight;

  private Question contextQuestion;
  private Param contextParam;

  private final Map<String, Boolean> sortingMap;

  // optionally override what is in the query set.  null means don't override
  private List<PostCacheUpdateSql> postCacheUpdateSqls;


  // =========================================================================
  // Abstract methods
  // =========================================================================

  protected abstract void appendChecksumJSON(JSONObject jsQuery, boolean extra) throws JSONException;

  protected abstract QueryInstance<? extends Query> makeInstance(RunnableObj<QueryInstanceSpec> paramValues) throws WdkModelException;

  @Override
  public abstract Query clone();

  public abstract boolean getIsCacheable();

  public abstract void resolveQueryReferences(WdkModel wdkModel) throws WdkModelException;

  // =========================================================================
  // Constructors
  // =========================================================================

  protected Query() {
    columnList = new ArrayList<>();
    columnMap = new LinkedHashMap<>();
    hasWeight = false;
    sortingMap = new LinkedHashMap<>();
    paramValuesSets = new ArrayList<>();
  }

  /**
   * clone the query object
   *
   * @param query existing query to clone
   */
  protected Query(Query query) {
    super(query);

    this.name = query.name;
    if (query.columnList != null)
      this.columnList = new ArrayList<>(query.columnList);
    this.columnMap = new LinkedHashMap<>();
    this.querySet = query.querySet;
    this.doNotTest = query.doNotTest;
    paramValuesSets = new ArrayList<>();
    this.paramValuesSets = new ArrayList<>(query.paramValuesSets);
    this.hasWeight = query.hasWeight;
    this.contextQuestion = query.getContextQuestion();
    this.contextParam = query.getContextParam();
    this.sortingMap = new LinkedHashMap<>(query.sortingMap);
    this.postCacheUpdateSqls = query.postCacheUpdateSqls == null
      ? null : new ArrayList<>(query.postCacheUpdateSqls);

    // clone columns
    query.columnMap.values()
      .stream()
      .map(Column::new)
      .peek(c -> c.setQuery(this))
      .forEach(c -> columnMap.put(c.getName(), c));
  }

  /**
   * @return the parameter that contains this query or null if this is an
   * independent query
   */
  public Param getContextParam() {
    return contextParam;
  }

  @Override
  public Map<String, Param> getRequiredParams() {
    if (contextParam == null)
      return getParamMap();

    // This is safe because of checks in the model parsing
    // that ensure the params declared as used by a query
    // with a context param are included in that params
    // depended params.
    var out = new HashMap<String, Param>();
    var qry = new LinkedList<>(getParamMap().values());

    while (!qry.isEmpty()) {
      var cur = qry.poll();

      out.put(cur.getName(), cur);
      qry.addAll(cur.getDependedParams());
    }

    return out;
  }

  public void setContextParam(Param contextParam) {
    this.contextParam = contextParam;
  }

  public Question getContextQuestion() {
    return contextQuestion;
  }

  /**
   * @throws WdkModelException if something goes wrong while assigning values
   * related to the context question
   */
  public void setContextQuestion(Question contextQuestion) throws WdkModelException {
    this.contextQuestion = contextQuestion;
    for (Param param : paramMap.values())
      param.setContextQuestion(contextQuestion);
  }

  public void setIndexColumns(String[] indexColumns) {
    this.indexColumns = indexColumns;
  }

  public String[] getIndexColumns() {
    return indexColumns;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public QuerySet getQuerySet() {
    return querySet;
  }

  public void setQuerySet(QuerySet querySet) {
    this.querySet = querySet;
  }

  @Override
  public String getFullName() {
    return ((querySet != null) ? querySet.getName() + "." : "") + name;
  }

  public void addColumn(Column column) throws WdkModelException {
    WdkModelException duplicationError = new WdkModelException("More than one column with name '" +
        column.getName() + "' added to Query '" + getFullName() + "'.");
    if (columnList != null) {
      if (columnList.stream().anyMatch(col -> col.getName().equals(column.getName()))) {
        throw duplicationError;
      }
      columnList.add(column);
    }
    else {
      if (columnMap.containsKey(column.getName())) {
        throw duplicationError;
      }
      columnMap.put(column.getName(), column);
    }
    column.setQuery(this);
  }

  public Map<String, Column> getColumnMap() {
    return new LinkedHashMap<>(columnMap);
  }

  public Column[] getColumns() {
    return columnMap.values().toArray(new Column[0]);
  }

  // exclude this query from sanity testing
  public void setDoNotTest(boolean doNotTest) {
    this.doNotTest = doNotTest;
  }

  @Override
  public boolean getDoNotTest() {
    return doNotTest;
  }

  @SuppressWarnings("unused") // ModelXmlParser
  public void addParamValuesSet(ParamValuesSet paramValuesSet) {
    paramValuesSets.add(paramValuesSet);
  }

  public List<ParamValuesSet> getRawParamValuesSets() {
    return paramValuesSets;
  }

  public String getChecksum(boolean extra) throws WdkModelException {
    try {
      JSONObject jsQuery = getChecksumJSON(extra);
      return EncryptionUtil.encrypt(JsonUtil.serialize(jsQuery));
    }
    catch (JSONException e) {
      throw new WdkModelException("Unable to get JSON content for checksum.", e);
    }
  }

  public List<PostCacheUpdateSql> getPostCacheUpdateSqls() {
    return postCacheUpdateSqls == null? null : Collections.unmodifiableList(postCacheUpdateSqls);
  }

  @SuppressWarnings("unused") // ModelXmlParser
  public void addPostCacheUpdateSql(PostCacheUpdateSql postCacheUpdateSql) {
    if (postCacheUpdateSqls == null) postCacheUpdateSqls = new ArrayList<>();
    postCacheUpdateSqls.add(postCacheUpdateSql);
  }

  /**
   * @param extra
   *   if extra is true, then column names are also includes, plus the extra
   *   info from param.
   *
   * @throws JSONException
   *   if unable to create JSON object
   */
  private JSONObject getChecksumJSON(boolean extra) throws JSONException {
    // use JSON to construct the string content
    JSONObject jsQuery = new JSONObject()
      .put("name", getFullName())
      .put("project", getWdkModel().getProjectId());

    // add context question name
    if (contextQuestion != null)
      jsQuery.put("contextQuestion", contextQuestion.getFullName());

    JSONArray jsParams = new JSONArray();
    paramMap.keySet()
      .stream()
      .sorted()
      .map(paramMap::get)
      .map(param -> param.getChecksumJSON(extra))
      .forEach(jsParams::put);
    jsQuery.put("params", jsParams);

    // construct columns; ordered by columnName
    if (extra) {
      JSONArray jsColumns = new JSONArray();
      columnMap.keySet()
        .stream()
        .sorted()
        .map(columnMap::get)
        .map(Column::getJSONContent)
        .forEach(jsColumns::put);

      jsQuery.put("columns", jsColumns);
    }

    // append child-specific data
    appendChecksumJSON(jsQuery, extra);

    return jsQuery;
  }

  @Override
  public void excludeResources(String projectId) throws WdkModelException {

    super.excludeResources(projectId);

    // exclude columns
    for (Column column : columnList) {
      if (column.include(projectId)) {
        column.excludeResources(projectId);
        String columnName = column.getName();
        if (columnMap.containsKey(columnName)) {
          throw new WdkModelException("The column '" + columnName +
            "' is duplicated in query " + getFullName());
        }
        else
          columnMap.put(columnName, column);
      }
    }
    columnList = null;

    // exclude paramValuesSets
    paramValuesSets = paramValuesSets.stream()
      .filter(p -> p.include(projectId))
      .collect(Collectors.toList());
  }

  @Override
  public void resolveReferences(WdkModel wdkModel) throws WdkModelException {

    if (_resolved) return;
    _resolved = true;

    super.resolveReferences(wdkModel);

    // resolve columns
    for (Column column : columnMap.values()) {
      String sortingColumn = column.getSortingColumn();
      if (sortingColumn == null)
        continue;
      if (!columnMap.containsKey(sortingColumn))
        throw new WdkModelException("Query [" + getFullName()
          + "] has a column [" + column.getName() + "] with sortingColumn ["
          + sortingColumn + "], but the sorting column doesn't exist in "
          + "the same query.");
    }

    // if the query is a transform, it has to return weight column.
    // this applies to both explicit transform and filter queries.
    if (getAnswerParamCount() == 1)
      if (!columnMap.containsKey(Utilities.COLUMN_WEIGHT))
        throw new WdkModelException("Transform query [" + getFullName() + "] "
          + "doesn't define the required " + Utilities.COLUMN_WEIGHT
          + " column.");

    resolveQueryReferences(wdkModel);

    // check the column names in the sorting map
    for (String column : sortingMap.keySet())
      if (!columnMap.containsKey(column))
        throw new WdkModelException("Invalid sorting column '" + column
          + "' in query " + getFullName());

    if (postCacheUpdateSqls != null)
      for (PostCacheUpdateSql postCacheUpdateSql : postCacheUpdateSqls)
        if (postCacheUpdateSql != null
          && (postCacheUpdateSql.getSql() == null
            || !postCacheUpdateSql.getSql().contains(Utilities.MACRO_CACHE_TABLE)
            || !postCacheUpdateSql.getSql().contains(Utilities.MACRO_CACHE_INSTANCE_ID)
        ))
          throw new WdkModelException(
            "Invalid PostCacheInsertSql. <sql> must be provided, and include the macros: "
              + Utilities.MACRO_CACHE_TABLE
              + " and "
              + Utilities.MACRO_CACHE_INSTANCE_ID);
  }

  public boolean isBoolean() {
    return (this instanceof BooleanQuery);
  }

  public int getAnswerParamCount() {
    return getAnswerParams().size();
  }

  public List<AnswerParam> getAnswerParams() {
    return paramMap.values().stream()
        .filter(AnswerParam.class::isInstance)
        .map(AnswerParam.class::cast)
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder(getFullName());
    buffer.append(": params{");
    boolean firstParam = true;
    for (Param param : paramMap.values()) {
      if (firstParam)
        firstParam = false;
      else
        buffer.append(", ");
      buffer.append(param.getName()).append("[");
      buffer.append(param.getClass().getSimpleName()).append("]");
    }
    buffer.append("} columns{");
    boolean firstColumn = true;
    for (Column column : columnMap.values()) {
      if (firstColumn)
        firstColumn = false;
      else
        buffer.append(", ");
      buffer.append(column.getName());
    }
    buffer.append("}");
    return buffer.toString();
  }

  /**
   * @param hasWeight
   *          the hasWeight to set
   */
  public void setHasWeight(boolean hasWeight) {
    this.hasWeight = hasWeight;
  }

  /**
   * @return the hasWeight
   */
  public boolean isHasWeight() {
    return hasWeight;
  }

  public void setSorting(String sorting) {
    sortingMap.clear();
    for (String clause : sorting.split(",")) {
      String[] term = clause.trim().split(" ", 2);
      String column = term[0];
      boolean order = (term.length == 1 || term[1].equalsIgnoreCase("ASC"));
      sortingMap.put(column, order);
    }
  }

  public Map<String, Boolean> getSortingMap() {
    return new LinkedHashMap<>(sortingMap);
  }

  /**
   * The only info we need for the query checksum is the columns to make sure we
   * have correct columns to store info we need.
   */
  public String getChecksum() throws WdkModelException {
    JSONObject jsQuery = new JSONObject();
    try {
      jsQuery.put("name", getFullName());

      JSONArray jsColumns = new JSONArray();
      for (Column column : getColumns()) {
        jsColumns.put(column.getJSONContent());
      }
      jsQuery.put("columns", jsColumns);
    }
    catch (JSONException ex) {
      throw new WdkModelException(ex);
    }
    return EncryptionUtil.encrypt(JsonUtil.serialize(jsQuery));
  }

  public final void printDependency(PrintWriter writer, String indent) throws WdkModelException {
    writer.println(indent + "<" + getClass().getSimpleName() + " name=\"" + getFullName() + "\">");
    String indent1 = indent + WdkModel.INDENT;
    String indent2 = indent1 + WdkModel.INDENT;

    // print params
    if (!paramMap.isEmpty()) {
      writer.println(indent1 + "<params size=\"" + paramMap.size() + "\">");
      String[] paramNames = paramMap.keySet().toArray(new String[0]);
      Arrays.sort(paramNames);
      for (String paramName : paramNames) {
        paramMap.get(paramName).printDependency(writer, indent2);
      }
      writer.println(indent1 + "</params>");
    }

    // print columns
    if (!columnMap.isEmpty()) {
      writer.println(indent1 + "<columns size=\"" + columnMap.size() + "\">");
      String[] columnNames = columnMap.keySet().toArray(new String[0]);
      Arrays.sort(columnNames);
      for (String columnName : columnNames) {
        columnMap.get(columnName).printDependency(writer, indent2);
      }
      writer.println(indent1 + "</columns>");
    }

    writer.println(indent + "</" + getClass().getSimpleName() + ">");
  }

  public Optional<AnswerParam> getPrimaryAnswerParam() {
    List<AnswerParam> params = getAnswerParams();
    return params.isEmpty() ? Optional.empty() : Optional.of(params.get(0));
  }

  public Optional<AnswerParam> getSecondaryAnswerParam() {
    List<AnswerParam> params = getAnswerParams();
    return params.size() > 1 ? Optional.of(params.get(1)) : Optional.empty();
  }

  /**
   * @throws WdkModelException if error occurs resolving column types 
   */
  public Map<String, AttributeFieldDataType> resolveColumnTypes() throws WdkModelException {
    return columnMap.entrySet()
      .stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> Optional.ofNullable(e.getValue().getType())
          .map(AttributeFieldDataType::fromColumnType)
          .orElse(AttributeFieldDataType.OTHER)));
  }
}
