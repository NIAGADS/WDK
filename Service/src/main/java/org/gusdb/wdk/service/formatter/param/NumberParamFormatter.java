package org.gusdb.wdk.service.formatter.param;

import org.gusdb.fgputil.validation.ValidObjectFactory.DisplayablyValid;
import org.gusdb.wdk.core.api.JsonKeys;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.query.param.NumberParam;
import org.gusdb.wdk.model.query.spec.ParameterContainerInstanceSpec;
import org.json.JSONObject;

public class NumberParamFormatter extends ParamFormatter<NumberParam> {

  NumberParamFormatter(NumberParam param) {
    super(param);
  }

  @Override
  public <S extends ParameterContainerInstanceSpec<S>> JSONObject getJson(DisplayablyValid<S> spec) throws WdkModelException {
    return getBaseJson(spec)

        .put(JsonKeys.MIN_VALUE, _param.getMin())
        .put(JsonKeys.MAX_VALUE, _param.getMax())
        .put(JsonKeys.INCREMENT, _param.getStep());  // aka the increment
  }
  
  @Override
  public String getParamType() {
    return JsonKeys.NUMBER_PARAM_TYPE;
  }

}
