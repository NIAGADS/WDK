package org.gusdb.wdk.model;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.wdk.model.dbms.ResultList;
import org.gusdb.wdk.model.query.Query;
import org.gusdb.wdk.model.query.QueryInstance;
import org.json.JSONException;
import org.json.JSONObject;

public class FlatVocabParam extends AbstractEnumParam {

    public static final String PARAM_SERVED_QUERY = "ServedQuery";

    private static final String COLUMN_TERM = "term";
    private static final String COLUMN_INTERNAL = "internal";
    private static final String COLUMN_DISPLAY = "display";
    private static final String COLUMN_PARENT_TERM = "parentTerm";

    private Query query;
    private String queryTwoPartName;
    private String servedQueryName = "unknown";

    public FlatVocabParam() {}

    public FlatVocabParam(FlatVocabParam param) {
        super(param);
        this.query = param.query;
        this.queryTwoPartName = param.queryTwoPartName;
    }

    // ///////////////////////////////////////////////////////////////////
    // /////////// Public properties ////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////

    public void setQueryRef(String queryTwoPartName) {

        this.queryTwoPartName = queryTwoPartName;
    }

    public Query getQuery() {
        return query;
    }

    /**
     * @param servedQueryName
     *            the servedQueryName to set
     */
    public void setServedQueryName(String servedQueryName) {
        this.servedQueryName = servedQueryName;
    }

    // ///////////////////////////////////////////////////////////////////
    // /////////// Protected properties ////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.gusdb.wdk.model.Param#resolveReferences(org.gusdb.wdk.model.WdkModel)
     */
    @Override
    public void resolveReferences(WdkModel model) throws WdkModelException {
        Query query = (Query) model.resolveReference(queryTwoPartName);
        query.resolveReferences(model);
        query = query.clone();

        // add a served query param into flatVocabQuery, if it doesn't exist
        ParamSet paramSet = model.getParamSet(Utilities.INTERNAL_PARAM_SET);
        StringParam param = new StringParam();
        param.setName(PARAM_SERVED_QUERY);
        param.setDefault(servedQueryName);
        param.setAllowEmpty(true);
        param.resolveReferences(model);
        param.setResources(model);
        paramSet.addParam(param);
        query.addParam(param);
        this.query = query;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.Param#setResources(org.gusdb.wdk.model.WdkModel)
     */
    @Override
    public void setResources(WdkModel model) throws WdkModelException {
        super.setResources(model);
    }

    protected synchronized void initVocabMap() throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException {

        if (termInternalMap == null) {

            // check if the query has "display" column
            boolean hasDisplay = query.getColumnMap().containsKey(
                    COLUMN_DISPLAY);
            boolean hasParent = query.getColumnMap().containsKey(
                    COLUMN_PARENT_TERM);

            Map<String, String> termParentMap = new LinkedHashMap<String, String>();

            // prepare param values
            Map<String, Object> values = new LinkedHashMap<String, Object>();
            values.put(PARAM_SERVED_QUERY, servedQueryName);

            QueryInstance instance = query.makeInstance(values);
            ResultList result = instance.getResults();
            termInternalMap = new LinkedHashMap<String, String>();
            termDisplayMap = new LinkedHashMap<String, String>();
            termTreeList = new ArrayList<EnumParamTermNode>();
            while (result.next()) {
                String term = result.get(COLUMN_TERM).toString();
                String value = result.get(COLUMN_INTERNAL).toString();
                String display = hasDisplay
                        ? result.get(COLUMN_DISPLAY).toString() : term;
                String parentTerm = null;
                if (hasParent) {
                    Object parent = result.get(COLUMN_PARENT_TERM);
                    parentTerm = (parent == null) ? null : parent.toString();
                }
                termParentMap.put(term, parentTerm);

                termInternalMap.put(term, value);
                termDisplayMap.put(term, display);
            }
            if (termInternalMap.size() == 0)
                throw new WdkModelException("No item returned by the query of"
                        + " FlatVocabParam " + getFullName());
            initTreeMap(termParentMap);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.Param#clone()
     */
    @Override
    public Param clone() {
        return new FlatVocabParam(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.Param#appendJSONContent(org.json.JSONObject)
     */
    @Override
    protected void appendJSONContent(JSONObject jsParam) throws JSONException {
        // add underlying query name to it
        jsParam.append("query", query.getFullName());
    }

    @Override
    public Object dependentValueToIndependentValue(Object dependentValue)
            throws WdkModelException, SQLException, JSONException,
            WdkUserException, NoSuchAlgorithmException {
        return dependentValue;
    }
}
