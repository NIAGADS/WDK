package org.gusdb.gus.wdk.model.implementation;

import org.gusdb.gus.wdk.model.QueryI;
import org.gusdb.gus.wdk.model.QueryParamsException;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class QueryInstance {

    Query query;
    boolean isCacheable;
    TreeMap values = new TreeMap();

    ///////////////////////////////////////////////////////////////////
    ///////  Public
    ///////////////////////////////////////////////////////////////////

    public Collection getValues() {
	return values.values();
    }

    public boolean getIsCacheable() {
	return isCacheable;
    }

    public void setIsCacheable(boolean isCacheable) {
	this.isCacheable =query.getIsCacheable().booleanValue() && isCacheable;
    }

    public QueryI getQuery() {
	return this.query;
    }

    public void setValues(Map values) throws QueryParamsException {
	this.values = new TreeMap(values);
	query.validateParamValues(values);
    }

    ///////////////////////////////////////////////////////////////////
    ///////  Protected
    ///////////////////////////////////////////////////////////////////

    protected QueryInstance (Query query) {
	this.isCacheable = query.getIsCacheable().booleanValue();
	this.query = query;
    }

}
