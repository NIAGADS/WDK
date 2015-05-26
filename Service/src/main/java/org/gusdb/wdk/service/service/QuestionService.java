package org.gusdb.wdk.service.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.question.QuestionSet;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.service.formatter.QuestionFormatter;
import org.json.JSONException;

@Path("/question")
@Produces(MediaType.APPLICATION_JSON)
public class QuestionService extends WdkService {

  @GET
  public Response getQuestions(
      @QueryParam("recordClass") String recordClassStr,
      @QueryParam("expandQuestions") Boolean expandQuestions,
      @QueryParam("expandParams") Boolean expandParams)
          throws JSONException, WdkModelException {
    try {
      return Response.ok(QuestionFormatter.getQuestionsJson(
          (recordClassStr == null || recordClassStr.isEmpty() ? getAllQuestions(getWdkModel()) :
            getQuestionsForRecordClasses(getWdkModel(), recordClassStr.split(","))),
          getFlag(expandQuestions), getFlag(expandParams)).toString()).build();
    }
    catch (IllegalArgumentException e) {
      return getBadRequestResponse(e.getMessage());
    }
  }

  private static List<Question> getQuestionsForRecordClasses(
      WdkModel wdkModel, String[] recordClassNames) throws IllegalArgumentException {
    try {
      List<Question> questions = new ArrayList<>();
      for (String rcName : recordClassNames) {
        RecordClass rc = wdkModel.getRecordClass(rcName);
        questions.addAll(Arrays.asList(wdkModel.getQuestions(rc)));
      }
      return questions;
    }
    catch (WdkModelException e) {
      throw new IllegalArgumentException("At least one passed record class name is incorrect.", e);
    }
  }

  // TODO: seems like this should be part of the model, but we need
  //       to consider XML questions, boolean questions, etc.
  private static List<Question> getAllQuestions(WdkModel wdkModel) {
    List<Question> questions = new ArrayList<>();
    for (QuestionSet qSet : wdkModel.getAllQuestionSets()) {
      questions.addAll(Arrays.asList(qSet.getQuestions()));
    }
    return questions;
  }

  @GET
  @Path("/{questionName}")
  public Response getQuestion(
      @PathParam("questionName") String questionName,
      @QueryParam("expandParams") Boolean expandParams)
          throws WdkUserException, WdkModelException {
    getWdkModelBean().validateQuestionFullName(questionName);
    Question question = getWdkModel().getQuestion(questionName);
    return Response.ok(QuestionFormatter.getQuestionJson(question,
        getFlag(expandParams)).toString()).build();
  }

  @GET
  @Path("/{questionName}/param")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getParamsForQuestion(@PathParam("questionName") String questionName)
      throws JSONException, WdkModelException, WdkUserException {
    getWdkModelBean().validateQuestionFullName(questionName);
    Question question = getWdkModel().getQuestion(questionName);
    return Response.ok(QuestionFormatter.getParamsJson(question, true).toString()).build();
  }
}
