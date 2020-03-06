package org.gusdb.wdk.model.answer.factory;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.single.SingleRecordAnswerValue;
import org.gusdb.wdk.model.answer.single.SingleRecordQuestion;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.User;

public class AnswerValueFactory {

  /**
   * make an answer with default page size and sorting
   */
  public static AnswerValue makeAnswer(RunnableObj<Step> validStep)
      throws WdkModelException {
    return makeAnswer(validStep.get().getUser(),
        Step.getRunnableAnswerSpec(validStep));
  }

  /**
   * make an answer with default page size and sorting
   */
  public static AnswerValue makeAnswer(User user, RunnableObj<AnswerSpec> validSpec)
      throws WdkModelException {
    Question question = validSpec.get().getQuestion();
    int pageStart = 1;
    int pageEnd = Utilities.DEFAULT_PAGE_SIZE;
    AnswerValue answerValue = makeAnswer(user, validSpec, pageStart, pageEnd, null);
    if (question.isFullAnswer()) {
      int resultSize = answerValue.getResultSizeFactory().getResultSize();
      if (resultSize > pageEnd)
        answerValue.setPageIndex(pageStart, resultSize);
    }
    return answerValue;
  }

  public static AnswerValue makeAnswer(User user, RunnableObj<AnswerSpec> validSpec,
      int startIndex, int endIndex, Map<String, Boolean> sortingMap) throws WdkModelException {
    Question question = validSpec.get().getQuestion();
    if (question instanceof SingleRecordQuestion) {
      return new SingleRecordAnswerValue(user, validSpec);
    }
    else {
      return new AnswerValue(user, validSpec, startIndex, endIndex, sortingMap);
    }
  }

  public static AnswerValue makeAnswer(AnswerValue answerValue,
      RunnableObj<AnswerSpec> modifiedSpec) throws WdkModelException {
    return makeAnswer(answerValue.getUser(), modifiedSpec, answerValue.getStartIndex(),
        answerValue.getEndIndex(), answerValue.getSortingMap());
  }

}
