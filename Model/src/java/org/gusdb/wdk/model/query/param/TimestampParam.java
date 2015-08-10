/**
 * 
 */
package org.gusdb.wdk.model.query.param;

import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.user.User;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The timestamp param represents a flag that use the current timestamp as input value every time a search is
 * run from question page, or a step is revised. However, if a step is reloaded, such as when user re-opens a
 * previous strategy, the value of the timestamp param stays the same.
 * 
 * The timestamp param is always hidden, and user is not allowed to input value to it. This param is used when
 * the data of the resource can be changed over time, while the query to the data is cached. We can use the
 * cache when the step is simply reloaded, but start a new cache when user runs a search or revise a step with
 * the same param values.
 * 
 * the raw, stable value, signature, and internal value for a timestampParam is the same.
 * 
 * @author xingao
 * 
 *         the four types of values are identical.
 */
public class TimestampParam extends Param {

  private long interval = 1;

  /**
     * 
     */
  public TimestampParam() {
    setHandler(new TimestampParamHandler());
  }

  /**
   * @param param
   */
  public TimestampParam(TimestampParam param) {
    super(param);
    this.interval = param.interval;
  }

  /**
   * The interval that the default value of timestampParam will be updated.
   * 
   * @return the interval
   */
  public long getInterval() {
    return interval;
  }

  /**
   * @param interval
   *          the interval to set
   */
  public void setInterval(long interval) {
    if (interval <= 0)
      interval = 1;
    this.interval = interval;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.query.param.Param#appendJSONContent(org.json.JSONObject )
   */
  @Override
  protected void appendJSONContent(JSONObject jsParam, boolean extra) throws JSONException {
    if (extra) {
      jsParam.put("interval", interval);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.query.param.Param#clone()
   */
  @Override
  public Param clone() {
    return new TimestampParam(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.query.param.Param#validateValue(org.gusdb.wdk.model .user.User,
   * java.lang.String)
   */
  @Override
  protected void validateValue(User user, String rawOrDependentValue, Map<String, String> contextValues)
      throws WdkModelException, WdkUserException {
    // nothing to validation. the value of timestamp can be any string
  }

  /**
   * it is always false (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.query.param.Param#isVisible()
   */
  @Override
  public boolean isVisible() {
    return false;
  }

  @Override
  public String getDefault() {
    long time = System.currentTimeMillis();
    String value = Long.toString(time / (1000 * interval));
    System.out.println(time + " - " + value);
    return value;
  }

  @Override
  protected void applySuggection(ParamSuggestion suggest) {
    // do nothing
  }

  @Override
  public String getBriefRawValue(Object rawValue, int truncateLength) throws WdkModelException {
    String value = (String) rawValue;
    if (value.length() > truncateLength)
      value = value.substring(0, truncateLength) + "...";
    return value;
  }
}