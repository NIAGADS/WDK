package org.gusdb.wdk.controller.action;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.ApplicationInitListener;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.gusdb.wdk.model.jspwrap.XmlAnswerValueBean;
import org.gusdb.wdk.model.jspwrap.XmlQuestionBean;
import org.gusdb.wdk.model.jspwrap.XmlQuestionSetBean;

/**
 * This Action is called by the ActionServlet when a WDK xml question is asked.
 * It 1) reads the question name param value,
 *    2) runs the xml query and saves the answer
 *    3) forwards control to a jsp page that displays the full result
 */

public class ShowXmlDataContentAction extends Action {
    public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response) throws Exception {
	String xmlQName = request.getParameter(CConstants.NAME);
	XmlQuestionBean xmlQuestion = getXmlQuestionByFullName(xmlQName);
	XmlAnswerValueBean xmlAnswerValue = xmlQuestion.getFullAnswerValue();
	request.setAttribute(CConstants.WDK_XMLANSWER_KEY, xmlAnswerValue);
	return getForward(xmlAnswerValue, mapping);
    }

    protected XmlQuestionBean getXmlQuestionByFullName(String qFullName) {
	int dotI = qFullName.indexOf('.');
	String qSetName = qFullName.substring(0, dotI);
	String qName = qFullName.substring(dotI+1, qFullName.length());

	WdkModelBean wdkModel = (WdkModelBean)getServlet().getServletContext().getAttribute(CConstants.WDK_MODEL_KEY);
	
	XmlQuestionSetBean wdkQuestionSet = (XmlQuestionSetBean)wdkModel.getXmlQuestionSetsMap().get(qSetName);
	XmlQuestionBean wdkQuestion = (XmlQuestionBean)wdkQuestionSet.getQuestionsMap().get(qName);
	return wdkQuestion;
    }

    private ActionForward getForward (XmlAnswerValueBean xmlAnswerValue, ActionMapping mapping) {
	ServletContext svltCtx = getServlet().getServletContext();
	String customViewDir = (String)svltCtx.getAttribute(CConstants.WDK_CUSTOMVIEWDIR_KEY);
	String customViewFile1 = customViewDir + File.separator
	    + xmlAnswerValue.getQuestion().getFullName() + ".jsp";
	String customViewFile2 = customViewDir + File.separator
	    + xmlAnswerValue.getRecordClass().getFullName() + ".jsp";
	ActionForward forward = null;
	if (ApplicationInitListener.resourceExists(customViewFile1, svltCtx)) {
	    forward = new ActionForward(customViewFile1);
	} else if (ApplicationInitListener.resourceExists(customViewFile2, svltCtx)) {
	    forward = new ActionForward(customViewFile2);
	} else {
	    forward = mapping.findForward(CConstants.SHOW_XMLDATA_CONTENT_MAPKEY);
	}
	return forward;
    }

}



