/**
 * 
 */
package org.gusdb.wdk.model.query.param;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.gusdb.fgputil.db.platform.DBPlatform;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.query.QueryInstance;
import org.gusdb.wdk.model.user.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author jerric
 * 
 */
public class FilterParamNewHandler extends AbstractParamHandler {

  public static final String LABELS_SUFFIX = "-labels";
  public static final String TERMS_SUFFIX = "-values";

  public static final String TERMS_KEY = "values";
  public static final String FILTERS_KEY = "filters";
  public static final String FILTERS_FIELD = "field";
  

  public FilterParamNewHandler() {}

  public FilterParamNewHandler(FilterParamNewHandler handler, Param param) {
    super(handler, param);
  }

  /**
   * the raw value is a JSON string, and same as the stable value.
   * 
   * @see org.gusdb.wdk.model.query.param.ParamHandlerPlugin#toStoredValue(org.gusdb .wdk.model.user.User,
   *      java.lang.String, java.util.Map)
   */
  @Override
  public String toStableValue(User user, Object rawValue, Map<String, String> contextParamValues) {
    return (String) rawValue;
  }

  /**
   * the raw value is a JSON string, and same as the stable value.
   * 
   * @see org.gusdb.wdk.model.query.param.ParamHandlerPlugin#toRawValue(org.gusdb .wdk.model.user.User,
   *      java.lang.String, java.util.Map)
   */
  @Override
  public String toRawValue(User user, String stableValue, Map<String, String> contextParamValues) {
    return stableValue;
  }

  /**
   * return SQL that runs the metadataQuery, including its depended params, and applies
   * the filters to it. 
   * 
   
    SELECT mf.internal
      FROM (${metadata_qc}) mf
      WHERE mf.ontology_term_id = 'age'
      AND mf.numeric_value      >= 66
      AND mf.numeric_value      <= 80
    INTERSECT
    SELECT mf.internal
      FROM mf.${metadata_qc} mf
      WHERE mf.ontology_term_id = 'mood'
      AND mf.string_value       IN ('confused', 'happy')
    INTERSECT
    SELECT mf.internal
      FROM (${metadata_qc}) mf
      WHERE mf.ontology_term_id = 'size'
      AND mf.string_value       IN ('large')

   * 
   * 
   * @throws WdkUserException
   * 
   * @see org.gusdb.wdk.model.query.param.ParamHandlerPlugin#transform(org.gusdb. wdk.model.user.User,
   *      java.lang.String, java.util.Map)
   */
  @Override
  public String toInternalValue(User user, String stableValue, Map<String, String> contextParamValues)
      throws WdkModelException {

    try {
      Map<String, OntologyItem> ontology = ((FilterParamNew) this.param).getOntology(user, contextParamValues);
      String metadataSql = getMetadataQuerySql(user, stableValue, contextParamValues);
      JSONObject jsValue = new JSONObject(stableValue);
      JSONArray jsFilters = jsValue.getJSONArray(FILTERS_KEY);
      String metadataTableName = "md";
      String filterSelect = "SELECT md.internal FROM (" + metadataSql + ") md WHERE md.ontology_term_id = ";
      StringBuilder filtersSql = new StringBuilder();
      for (int i = 0; i < jsFilters.length(); i++) {
        if (i > 0) filtersSql.append(" INTERSECT ");
        JSONObject jsFilter = jsFilters.getJSONObject(i);
        filtersSql.append(filterSelect + "'" + jsFilter.getString(FILTERS_FIELD) + "' ");
        filtersSql.append(getFilterAsAndClause(jsFilter, metadataSql, ontology, metadataTableName));
      }
      return filtersSql.toString();
    }
    catch (JSONException | WdkUserException ex) {
      throw new WdkModelException(ex);
    }
  }
 
  private String getFilterAsAndClause(JSONObject jsFilter, String metadataSql, Map<String, OntologyItem> ontology, String metadataTableName) throws WdkModelException, WdkUserException {
    OntologyItem ontologyItem = ontology.get(jsFilter.getString("field"));
    
    switch(ontologyItem.getType()) {
      
    case OntologyItem.TYPE_STRING:
      JSONArray values = jsFilter.getJSONArray("value");
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < values.length(); j++) {
        String val = (values.get(j) == JSONObject.NULL)? "unknown" : values.getString(j);
        if (j != 0) sb.append(",");
        sb.append(val);
      }
      return "AND " + metadataTableName + ".string_value IN (" + sb + ") ";
      
    case OntologyItem.TYPE_NUMBER:
      return getRangeAndClause(jsFilter, "number", metadataTableName);
      
    case OntologyItem.TYPE_DATE:
    default:
      return getRangeAndClause(jsFilter, "number", metadataTableName);
    }
  }
  
  private String getRangeAndClause(JSONObject jsFilter, String dateOrNumber, String metadataTableName) {
    JSONObject range = jsFilter.getJSONObject("value");
    String min = range.getString("min");
    String max = range.getString("max");
    return "AND " + metadataTableName + "." + dateOrNumber + "_value > " + min + "AND " + metadataTableName + ".date_value < " + max; 
  }

  private String getMetadataQuerySql(User user, String stableValue, Map<String, String> contextParamValues) throws WdkModelException, WdkUserException {
    FilterParamNew filterParam = (FilterParamNew)param;
    QueryInstance<?> instance = MetaDataItemFetcher.getQueryInstance(user, contextParamValues, filterParam.getMetadataQuery());
    return instance.getSql();
  }

  /**
   * the signature is a checksum of sorted stable value.
   * 
   * @throws WdkModelException
   * @throws WdkUserException 
   * 
   * @see org.gusdb.wdk.model.query.param.ParamHandler#toSignature(org.gusdb.wdk.model.user.User,
   *      java.lang.String, java.util.Map)
   */
  @Override
  public String toSignature(User user, String stableValue, Map<String, String> contextParamValues)
      throws WdkModelException, WdkUserException {
    stableValue = normalizeStableValue(stableValue);  // sort it for stability
    try {
      JSONObject jsValue = new JSONObject(stableValue);
      JSONArray jsTerms = jsValue.getJSONArray(TERMS_KEY);
      String[] terms = new String[jsTerms.length()];
      for (int i = 0; i < terms.length; i++) {
        terms[i] = jsTerms.getString(i);
      }

      // return stable values, instead of list of terms
      if (param.isNoTranslation()) {
        return stableValue;
      }

      Arrays.sort(terms);
      return Utilities.encrypt(Arrays.toString(terms));
    }
    catch (JSONException ex) {
      throw new WdkModelException(ex);
    }

  }

  /**
   * raw value is a String[] of terms
   * 
   * @see org.gusdb.wdk.model.query.param.ParamHandler#getStableValue(org.gusdb.wdk.model.user.User,
   *      org.gusdb.wdk.model.query.param.RequestParams)
   */
  @Override
  public String getStableValue(User user, RequestParams requestParams) throws WdkUserException,
      WdkModelException {
    return cleanAndValidateStableValue(user, requestParams.getParam(param.getName()));
  }
  
  @Override
  public String cleanAndValidateStableValue(User user, String inputStableValue) throws WdkUserException, WdkModelException {
    String stableValue = inputStableValue;
    if (stableValue == null || stableValue.length() == 0) {
      // use empty value if needed
      if (!param.isAllowEmpty())
        throw new WdkUserException("The input to parameter '" + param.getPrompt() + "' is required.");

      stableValue = param.getDefault();
    }
    stableValue = normalizeStableValue(stableValue);
    return stableValue;
  }
  
  @Override
  public void prepareDisplay(User user, RequestParams requestParams, Map<String, String> contextParamValues)
      throws WdkModelException, WdkUserException {
    AbstractEnumParam aeParam = (AbstractEnumParam) param;

    // set labels
    Map<String, String> displayMap = aeParam.getDisplayMap(user, contextParamValues);
    String[] terms = displayMap.keySet().toArray(new String[0]);
    String[] labels = displayMap.values().toArray(new String[0]);
    requestParams.setArray(param.getName() + LABELS_SUFFIX, labels);
    requestParams.setArray(param.getName() + TERMS_SUFFIX, terms);

    // get the stable value
    String stableValue = requestParams.getParam(param.getName());
    Set<String> values = new HashSet<>();
    if (stableValue == null) { // stable value not set, use default
      stableValue = aeParam.getDefault(user, contextParamValues);
    }
    stableValue = normalizeStableValue(stableValue);
    Set<String> invalidValues = new HashSet<>();
    JSONObject jsValue = new JSONObject(stableValue);
    JSONArray jsTerms = jsValue.getJSONArray(TERMS_KEY);
    for (int i = 0; i < jsTerms.length(); i++) {
      String term = jsTerms.getString(i);
      if (displayMap.containsKey(term))
        values.add(term);
      else
        invalidValues.add(term);
    }
    // store the invalid values
    String[] invalids = invalidValues.toArray(new String[0]);
    Arrays.sort(invalids);
    requestParams.setAttribute(param.getName() + Param.INVALID_VALUE_SUFFIX, invalids);

    // set the stable & raw value
    if (stableValue != null)
      requestParams.setParam(param.getName(), stableValue);

    if (values.size() > 0) {
      String[] rawValue = values.toArray(new String[0]);
      requestParams.setArray(param.getName(), rawValue);
      requestParams.setAttribute(param.getName() + Param.RAW_VALUE_SUFFIX, rawValue);
    }
  }

  @Override
  public ParamHandler clone(Param param) {
    return new FilterParamNewHandler(this, param);
  }

  @Override
  public String getDisplayValue(User user, String stableValue, Map<String, String> contextParamValues)
      throws WdkModelException {
    stableValue = normalizeStableValue(stableValue);
    JSONObject jsValue = new JSONObject(stableValue);
    JSONArray jsFilters = jsValue.getJSONArray(FILTERS_KEY);
    try {
      Map<String, Map<String, String>> metadataSpec = ((FilterParam) this.param).getMetadataSpec(user, contextParamValues);
      if (jsFilters.length() == 0)
        return "All " + param.prompt;
      else {
        String display = "";
        for (int i = 0; i < jsFilters.length(); i++) {
          JSONObject jsFilter = jsFilters.getJSONObject(i);
          Map<String, String> fieldSpec = metadataSpec.get(jsFilter.getString("field"));
          display += fieldSpec.get("display") + " is ";
          switch(fieldSpec.get("filter")) {
          case "membership":
            JSONArray values = jsFilter.getJSONArray("value");
            for (int j = 0; j < values.length(); j++) {
              if (values.get(j) == JSONObject.NULL) values.put(j, "uknown");
            }
            display += jsFilter.getJSONArray("value").join(", ");
            break;
          case "range":
            JSONObject range = jsFilter.getJSONObject("value");
            String min = range.getString("min");
            String max = range.getString("max");
            display += min == null ? "less than " + max
                     : max == null ? "greater than " + min
                     : "between " + min + " and " + max;
            break;
          }
          if (i != jsFilters.length()) display += "\n";
        }
        return display;
      }
    } catch (WdkUserException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return jsValue.getJSONArray(TERMS_KEY).toString();
    }

  }
  
  private String normalizeStableValue(String stableValue) {
    // TODO sort stable value JSON
    return stableValue;
    
  }

}
