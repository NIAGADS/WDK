package org.gusdb.wdk.model.answer.single;

import java.util.Map;

import org.gusdb.fgputil.MapBuilder;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.query.Query;
import org.gusdb.wdk.model.query.SqlQuery;
import org.gusdb.wdk.model.query.param.Param;
import org.gusdb.wdk.model.question.DynamicAttributeSet;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.RecordClass;

public class SingleRecordQuestion extends Question {

  public static final String SINGLE_RECORD_QUESTION_PREFIX = "single_record_question_";

  public static String getQuestionName(RecordClass recordClass) {
    return getInternalQuestionName(SINGLE_RECORD_QUESTION_PREFIX, recordClass);
  }

  private final SingleRecordQuestionParam _param;

  public SingleRecordQuestion(RecordClass recordClass) {
    _wdkModel = recordClass.getWdkModel();
    setRecordClass(recordClass);
    setName(getQuestionName(recordClass));
    setDisplayName("Single " + recordClass.getDisplayName());
    _dynamicAttributeSet = new DynamicAttributeSet();
    _dynamicAttributeSet.setQuestion(this);
    _param = new SingleRecordQuestionParam(recordClass);
  }

  @Override
  public Query getQuery() {
    WdkModel wdkModel = _wdkModel;
    return new SqlQuery(){
      @Override
      public Map<String, Param> getParamMap() {
        return new MapBuilder<String,Param>(_param.getName(), _param).toMap();
      }
      @Override
      public WdkModel getWdkModel() {
        return wdkModel;
      }
    };
  }

  @Override
  public Param[] getParams() {
    return new Param[]{ _param };
  }

  @Override
  public boolean isBoolean() {
    return false;
  }

  public SingleRecordQuestionParam getParam() {
    return _param;
  }
}
