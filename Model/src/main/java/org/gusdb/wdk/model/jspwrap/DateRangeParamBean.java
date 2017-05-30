/**
 * 
 */
package org.gusdb.wdk.model.jspwrap;

import org.gusdb.wdk.model.query.param.DateRangeParam;

/**
 * @author jerric
 * 
 */
public class DateRangeParamBean extends ParamBean<DateRangeParam> {

    private DateRangeParam _param;
    /**
     * @param param
     */
    public DateRangeParamBean(DateRangeParam param) {
        super(param);
        _param = param;
    }
    
    public String getMinDate() {
      return _param.getMinDate();
    }
    
    public String getMaxDate() {
      return _param.getMaxDate();
    }

    public String getRegex() {
        return _param.getRegex();
    }
}
