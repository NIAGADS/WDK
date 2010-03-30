package org.gusdb.wdk.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.gusdb.wdk.model.query.Column;
import org.gusdb.wdk.model.query.ProcessQuery;
import org.gusdb.wdk.model.query.QuerySet;
import org.gusdb.wdk.model.query.SqlQuery;
import org.gusdb.wdk.model.query.param.AnswerParam;
import org.gusdb.wdk.model.query.param.DatasetParam;
import org.gusdb.wdk.model.query.param.EnumItem;
import org.gusdb.wdk.model.query.param.EnumItemList;
import org.gusdb.wdk.model.query.param.EnumParam;
import org.gusdb.wdk.model.query.param.FlatVocabParam;
import org.gusdb.wdk.model.query.param.ParamConfiguration;
import org.gusdb.wdk.model.query.param.ParamReference;
import org.gusdb.wdk.model.query.param.ParamSet;
import org.gusdb.wdk.model.query.param.ParamSuggestion;
import org.gusdb.wdk.model.query.param.ParamValuesSet;
import org.gusdb.wdk.model.query.param.StringParam;
import org.gusdb.wdk.model.query.param.TypeAheadParam;
import org.gusdb.wdk.model.query.param.TimestampParam;
import org.gusdb.wdk.model.xml.XmlAttributeField;
import org.gusdb.wdk.model.xml.XmlQuestion;
import org.gusdb.wdk.model.xml.XmlQuestionSet;
import org.gusdb.wdk.model.xml.XmlRecordClass;
import org.gusdb.wdk.model.xml.XmlRecordClassSet;
import org.gusdb.wdk.model.xml.XmlTableField;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ModelXmlParser extends XmlParser {

    private static final Logger logger = Logger.getLogger(ModelXmlParser.class);

    private URL xmlSchemaURL;
    private String xmlDataDir;

    public ModelXmlParser(String gusHome) throws SAXException, IOException {
        super(gusHome, "lib/rng/wdkModel.rng");

        // get model schema file and xml schema file
        xmlSchemaURL = makeURL(gusHome, "lib/rng/xmlAnswer.rng");
        xmlDataDir = gusHome + "/lib/xml/";
    }

    public WdkModel parseModel(String projectId)
            throws ParserConfigurationException,
            TransformerFactoryConfigurationError, TransformerException,
            IOException, SAXException, WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException, InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        logger.debug("Parsing model...");

        // get model config
        ModelConfig config = getModelConfig(projectId);
        String modelName = config.getModelName();

        // construct urls to model file, prop file, and config file
        URL modelURL = makeURL(gusHome, "lib/wdk/" + modelName + ".xml");
        URL modelPropURL = makeURL(gusHome, "config/" + projectId
                + "/model.prop");

        // validate the master model file
        logger.debug("Validating model files...");
        if (!validate(modelURL))
            throw new WdkModelException("Master model validation failed.");

        logger.debug("Combining & preparing DOM...");

        // replace any <import> tag with content from sub-models in the
        // master model, and build the master document
        Document masterDoc = buildMasterDocument(modelURL);

        // load property map
        Map<String, String> properties = getPropMap(modelPropURL);

        // add several config into the prop map automatically
        if (!properties.containsKey("PROJECT_ID")) {
            properties.put("PROJECT_ID", projectId);
        }
        if (!properties.containsKey("USER_DBLINK")) {
            String userDbLink = config.getAppDB().getUserDbLink();
            properties.put("USER_DBLINK", userDbLink);
        }
        if (!properties.containsKey("USER_SCHEMA")) {
            properties.put("USER_SCHEMA", config.getUserDB().getUserSchema());
        }
        if (!properties.containsKey("WDK_ENGINE_SCHEMA")) {
            String engineSchema = config.getUserDB().getWdkEngineSchema();
            properties.put("WDK_ENGINE_SCHEMA", engineSchema);
        }

        Set<String> replacedMacros = new LinkedHashSet<String>();
        InputStream modelXmlStream = substituteProps(masterDoc, properties,
                replacedMacros);

        logger.debug("Parsing model DOM...");
        WdkModel model = (WdkModel) digester.parse(modelXmlStream);

        model.setXmlSchema(xmlSchemaURL); // set schema for xml data
        model.setXmlDataDir(new File(xmlDataDir)); // consider refactoring
        model.configure(config);
        model.setResources();
        model.setProperties(properties, replacedMacros);

        return model;
    }

    private ModelConfig getModelConfig(String projectId) throws SAXException,
            IOException, WdkModelException {
        ModelConfigParser parser = new ModelConfigParser(gusHome);
        return parser.parseConfig(projectId);
    }

    private Document buildMasterDocument(URL wdkModelURL) throws SAXException,
            IOException, ParserConfigurationException, WdkModelException {
        // get the xml document of the model
        Document masterDoc = buildDocument(wdkModelURL);
        Node rootNode = masterDoc.getElementsByTagName("wdkModel").item(0);

        // get all imports, and replace each of them with the sub-model
        NodeList importNodes = masterDoc.getElementsByTagName("import");
        for (int i = 0; i < importNodes.getLength(); i++) {
            // get url to the first import
            Node importNode = importNodes.item(i);
            String href = importNode.getAttributes().getNamedItem("file").getNodeValue();
            URL importURL = makeURL(gusHome, "lib/wdk/" + href);

            // validate the sub-model
            if (!validate(importURL))
                throw new WdkModelException("sub model "
                        + importURL.toExternalForm() + " validation failed.");

            // logger.debug("Importing: " + importURL.toExternalForm());

            Document importDoc = buildDocument(importURL);

            // get the children nodes from imported sub-model, and add them
            // into master document
            Node subRoot = importDoc.getElementsByTagName("wdkModel").item(0);
            NodeList childrenNodes = subRoot.getChildNodes();
            for (int j = 0; j < childrenNodes.getLength(); j++) {
                Node childNode = childrenNodes.item(j);
                if (childNode instanceof Element) {
                    Node imported = masterDoc.importNode(childNode, true);
                    rootNode.appendChild(imported);
                }
            }
        }
        return masterDoc;
    }

    private Map<String, String> getPropMap(URL modelPropURL) throws IOException {
        Map<String, String> propMap = new LinkedHashMap<String, String>();
        Properties properties = new Properties();
        properties.load(modelPropURL.openStream());
        Iterator<Object> it = properties.keySet().iterator();
        while (it.hasNext()) {
            String propName = (String) it.next();
            String value = properties.getProperty(propName);
            propMap.put(propName, value);
        }
        return propMap;
    }

    private InputStream substituteProps(Document masterDoc,
            Map<String, String> properties, Set<String> replacedMacros)
            throws TransformerFactoryConfigurationError, TransformerException,
            WdkModelException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // transform the DOM doc to a string
        Source source = new DOMSource(masterDoc);
        Result result = new StreamResult(out);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(source, result);
        String content = new String(out.toByteArray());

        Pattern pattern = Pattern.compile("\\@([\\w\\.\\-]+)\\@",
                Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        // search and substitute the property macros
        StringBuffer buffer = new StringBuffer();
        int prevPos = 0;
        while (matcher.find()) {
            String propName = matcher.group(1);

            // check if the property macro is defined
            if (!properties.containsKey(propName)) continue;

            String propValue = properties.get(propName);
            buffer.append(content.subSequence(prevPos, matcher.start()));
            buffer.append(propValue);
            prevPos = matcher.end();

            replacedMacros.add(propName);
        }
        if (prevPos < content.length())
            buffer.append(content.substring(prevPos));

        // construct input stream
        return new ByteArrayInputStream(buffer.toString().getBytes());
    }

    protected Digester configureDigester() {
        Digester digester = new Digester();
        digester.setValidating(false);

        // Root -- WDK Model
        digester.addObjectCreate("wdkModel", WdkModel.class);
        digester.addSetProperties("wdkModel");

        configureNode(digester, "wdkModel/modelName", WdkModelName.class,
                "addWdkModelName");

        configureNode(digester, "wdkModel/introduction", WdkModelText.class,
                "addIntroduction");
        digester.addCallMethod("wdkModel/introduction", "setText", 0);

        // default property list
        configureNode(digester, "wdkModel/defaultPropertyList",
                PropertyList.class, "addDefaultPropertyList");

        configureNode(digester, "wdkModel/defaultPropertyList/value",
                WdkModelText.class, "addValue");
        digester.addCallMethod("wdkModel/defaultPropertyList/value", "setText",
                0);

        // categories
        configureNode(digester, "wdkModel/category", Category.class,
                "addCategory");

        configureNode(digester, "wdkModel/category/questionRef",
                CategoryQuestionRef.class, "addQuestionRef");
        digester.addCallMethod("wdkModel/category/questionRef", "setText", 0);

        // configure property macros
        configureNode(digester, "wdkModel/declaredMacro",
                MacroDeclaration.class, "addMacroDeclaration");

        // configure all sub nodes of recordClassSet
        configureRecordClassSet(digester);

        // configure all sub nodes of querySet
        configureQuerySet(digester);

        // configure all sub nodes of paramSet
        configureParamSet(digester);

        // configure all sub nodes of questionSet
        configureQuestionSet(digester);

        // configure all sub nodes of xmlQuestionSet
        configureXmlQuestionSet(digester);

        // configure all sub nodes of xmlRecordSet
        configureXmlRecordClassSet(digester);

        // configure all sub nodes of xmlRecordSet
        configureGroupSet(digester);

        // configure query monitor
        configureQueryMonitor(digester);

        return digester;
    }

    private void configureRecordClassSet(Digester digester) {
        // record class set
        configureNode(digester, "wdkModel/recordClassSet",
                RecordClassSet.class, "addRecordClassSet");

        // record class
        configureNode(digester, "wdkModel/recordClassSet/recordClass",
                RecordClass.class, "addRecordClass");

        // primary key attribute
        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/primaryKeyAttribute",
                PrimaryKeyAttributeField.class, "addAttributeField");
        configureNode(
                digester,
                "wdkModel/recordClassSet/recordClass/primaryKeyAttribute/columnRef",
                WdkModelText.class, "addColumnRef");
        digester.addCallMethod(
                "wdkModel/recordClassSet/recordClass/primaryKeyAttribute/columnRef",
                "setText", 0);
        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/primaryKeyAttribute/text",
                WdkModelText.class, "addText");
        digester.addCallMethod(
                "wdkModel/recordClassSet/recordClass/primaryKeyAttribute/text",
                "setText", 0);

        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/attributesList",
                AttributeList.class, "addAttributeList");

        // defaultTestParamValues
        configureParamValuesSet(digester,
                "wdkModel/recordClassSet/recordClass/testParamValues",
                "addParamValuesSet");

        // reporter
        configureNode(digester, "wdkModel/recordClassSet/recordClass/reporter",
                ReporterRef.class, "addReporterRef");
        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/reporter/property",
                ReporterProperty.class, "addProperty");
        digester.addCallMethod(
                "wdkModel/recordClassSet/recordClass/reporter/property",
                "setValue", 0);

        // filter layouts
        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/answerFilterLayout",
                AnswerFilterLayout.class, "addFilterLayout");
        configureNode(
                digester,
                "wdkModel/recordClassSet/recordClass/answerFilterLayout/description",
                WdkModelText.class, "addDescription");
        digester.addCallMethod(
                "wdkModel/recordClassSet/recordClass/answerFilterLayout/description",
                "setText", 0);
        configureNode(
                digester,
                "wdkModel/recordClassSet/recordClass/answerFilterLayout/instanceRef",
                AnswerFilterInstanceReference.class, "addReference");
        configureNode(
                digester,
                "wdkModel/recordClassSet/recordClass/answerFilterLayout/layout",
                WdkModelText.class, "addLayout");
        digester.addCallMethod(
                "wdkModel/recordClassSet/recordClass/answerFilterLayout/layout",
                "setText", 0);

        // filter instances
        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/answerFilter",
                AnswerFilter.class, "addFilter");
        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/answerFilter/instance",
                AnswerFilterInstance.class, "addInstance");

        configureNode(
                digester,
                "wdkModel/recordClassSet/recordClass/answerFilter/instance/displayName",
                WdkModelText.class, "addDisplayName");
        digester.addCallMethod(
                "wdkModel/recordClassSet/recordClass/answerFilter/instance/displayName",
                "setText", 0);

        configureNode(
                digester,
                "wdkModel/recordClassSet/recordClass/answerFilter/instance/description",
                WdkModelText.class, "addDescription");
        digester.addCallMethod(
                "wdkModel/recordClassSet/recordClass/answerFilter/instance/description",
                "setText", 0);

        configureNode(
                digester,
                "wdkModel/recordClassSet/recordClass/answerFilter/instance/paramValue",
                WdkModelText.class, "addParamValue");
        digester.addCallMethod(
                "wdkModel/recordClassSet/recordClass/answerFilter/instance/paramValue",
                "setText", 0);

        // attribute query ref
        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/attributeQueryRef",
                AttributeQueryReference.class, "addAttributesQueryRef");

        configureNode(
                digester,
                "wdkModel/recordClassSet/recordClass/attributeQueryRef/columnAttribute",
                ColumnAttributeField.class, "addAttributeField");

        configureLinkTextFields(digester,
                "wdkModel/recordClassSet/recordClass/attributeQueryRef/");

        // tables
        configureNode(digester, "wdkModel/recordClassSet/recordClass/table",
                TableField.class, "addTableField");

        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/table/description",
                WdkModelText.class, "addDescription");
        digester.addCallMethod(
                "wdkModel/recordClassSet/recordClass/table/description",
                "setText", 0);

        // tableField's property list
        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/table/propertyList",
                PropertyList.class, "addPropertyList");

        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/table/propertyList/value",
                WdkModelText.class, "addValue");
        digester.addCallMethod(
                "wdkModel/recordClassSet/recordClass/table/propertyList/value",
                "setText", 0);

        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/table/columnAttribute",
                ColumnAttributeField.class, "addAttributeField");

        configureLinkTextFields(digester,
                "wdkModel/recordClassSet/recordClass/table/");

        // direct attribute fields in teh record class
        configureLinkTextFields(digester,
                "wdkModel/recordClassSet/recordClass/");

        // nested record and record list
        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/nestedRecord",
                NestedRecord.class, "addNestedRecordQuestionRef");

        configureNode(digester,
                "wdkModel/recordClassSet/recordClass/nestedRecordList",
                NestedRecordList.class, "addNestedRecordListQuestionRef");
    }

    private void configureQuerySet(Digester digester) {
        // QuerySet
        configureNode(digester, "wdkModel/querySet", QuerySet.class,
                "addQuerySet");

        // defaultTestParamValues
        configureParamValuesSet(digester,
                "wdkModel/querySet/defaultTestParamValues",
                "addDefaultParamValuesSet");

        // cardinalitySql
        configureNode(digester, "wdkModel/querySet/testRowCountSql",
                WdkModelText.class, "addTestRowCountSql");
        digester.addCallMethod("wdkModel/querySet/testRowCountSql", "setText",
                0);

        // sqlQuery
        configureNode(digester, "wdkModel/querySet/sqlQuery", SqlQuery.class,
                "addQuery");

        // testParamValues
        configureParamValuesSet(digester,
                "wdkModel/querySet/sqlQuery/testParamValues",
                "addParamValuesSet");

        configureNode(digester, "wdkModel/querySet/sqlQuery/sql",
                WdkModelText.class, "addSql");
        digester.addCallMethod("wdkModel/querySet/sqlQuery/sql", "setText", 0);

        configureNode(digester, "wdkModel/querySet/sqlQuery/paramRef",
                ParamReference.class, "addParamRef");

        configureNode(digester, "wdkModel/querySet/sqlQuery/column",
                Column.class, "addColumn");

        configureNode(digester, "wdkModel/querySet/sqlQuery/sqlParamValue",
                WdkModelText.class, "addSqlParamValue");
        digester.addCallMethod("wdkModel/querySet/sqlQuery/sqlParamValue",
                "setText", 0);

        // processQuery
        configureNode(digester, "wdkModel/querySet/processQuery",
                ProcessQuery.class, "addQuery");

        // testParamValues
        configureParamValuesSet(digester,
                "wdkModel/querySet/processQuery/testParamValues",
                "addParamValuesSet");

        configureNode(digester, "wdkModel/querySet/processQuery/paramRef",
                ParamReference.class, "addParamRef");

        configureNode(digester, "wdkModel/querySet/processQuery/wsColumn",
                Column.class, "addColumn");
    }

    private void configureParamSet(Digester digester) {
        // ParamSet
        configureNode(digester, "wdkModel/paramSet", ParamSet.class,
                "addParamSet");

        configureNode(digester, "wdkModel/paramSet/useTermOnly",
                ParamConfiguration.class, "addUseTermOnly");

        // string param
        String path = "wdkModel/paramSet/stringParam";
        configureNode(digester, path, StringParam.class, "addParam");
        configureParamContent(digester, path);
        configureNode(digester, path + "/regex", WdkModelText.class, "addRegex");
        digester.addCallMethod(path + "/regex", "setText", 0);

        // typeAhead param
        path = "wdkModel/paramSet/typeAheadParam";
        configureNode(digester, path, TypeAheadParam.class, "addParam");
        configureParamContent(digester, path);

        // flatVocabParam
        path = "wdkModel/paramSet/flatVocabParam";
        configureNode(digester, path, FlatVocabParam.class, "addParam");
        configureParamContent(digester, path);
        configureNode(digester, path + "/useTermOnly",
                ParamConfiguration.class, "addUseTermOnly");

        // answer param
        configureNode(digester, "wdkModel/paramSet/answerParam",
                AnswerParam.class, "addParam");
        configureParamContent(digester, "wdkModel/paramSet/answerParam");

        // dataset param
        path = "wdkModel/paramSet/datasetParam";
        configureNode(digester, path, DatasetParam.class, "addParam");
        configureParamContent(digester, path);

        // enum param
        path = "wdkModel/paramSet/enumParam";
        configureNode(digester, path, EnumParam.class, "addParam");
        configureParamContent(digester, path);

        configureNode(digester, path + "/useTermOnly",
                ParamConfiguration.class, "addUseTermOnly");

        path = path + "/enumList";
        configureNode(digester, path, EnumItemList.class, "addEnumItemList");

        configureNode(digester, path + "/useTermOnly",
                ParamConfiguration.class, "addUseTermOnly");

        configureNode(digester, path + "/enumValue", EnumItem.class,
                "addEnumItem");
        digester.addBeanPropertySetter(path + "/enumValue/display");
        digester.addBeanPropertySetter(path + "/enumValue/term");
        digester.addBeanPropertySetter(path + "/enumValue/internal");
        digester.addBeanPropertySetter(path + "/enumValue/parentTerm");

        configureNode(digester, path + "/enumValue/dependedValue",
                WdkModelText.class, "addDependedValue");
        digester.addCallMethod(path + "/enumValue/dependedValue", "setText", 0);

        // timestamp param
        path = "wdkModel/paramSet/timestampParam";
        configureNode(digester, path, TimestampParam.class, "addParam");
        configureParamContent(digester, path);
    }

    private void configureParamContent(Digester digester, String paramPath) {
        configureNode(digester, paramPath + "/help", WdkModelText.class,
                "addHelp");
        digester.addCallMethod(paramPath + "/help", "setText", 0);

        configureNode(digester, paramPath + "/suggest", ParamSuggestion.class,
                "addSuggest");
    }

    private void configureQuestionSet(Digester digester) {
        // QuestionSet
        configureNode(digester, "wdkModel/questionSet", QuestionSet.class,
                "addQuestionSet");

        configureNode(digester, "wdkModel/questionSet/description",
                WdkModelText.class, "addDescription");
        digester.addCallMethod("wdkModel/questionSet/description", "setText", 0);

        // question
        configureNode(digester, "wdkModel/questionSet/question",
                Question.class, "addQuestion");

        configureNode(digester, "wdkModel/questionSet/question/description",
                WdkModelText.class, "addDescription");
        digester.addCallMethod("wdkModel/questionSet/question/description",
                "setText", 0);

        configureNode(digester, "wdkModel/questionSet/question/summary",
                WdkModelText.class, "addSummary");
        digester.addCallMethod("wdkModel/questionSet/question/summary",
                "setText", 0);

        configureNode(digester, "wdkModel/questionSet/question/help",
                WdkModelText.class, "addHelp");
        digester.addCallMethod("wdkModel/questionSet/question/help", "setText",
                0);

        // question's property list
        configureNode(digester, "wdkModel/questionSet/question/propertyList",
                PropertyList.class, "addPropertyList");

        configureNode(digester,
                "wdkModel/questionSet/question/propertyList/value",
                WdkModelText.class, "addValue");
        digester.addCallMethod(
                "wdkModel/questionSet/question/propertyList/value", "setText",
                0);

        configureNode(digester, "wdkModel/questionSet/question/attributesList",
                AttributeList.class, "addAttributeList");

        // dynamic attribute set
        configureNode(digester,
                "wdkModel/questionSet/question/dynamicAttributes",
                DynamicAttributeSet.class, "addDynamicAttributeSet");

        configureNode(
                digester,
                "wdkModel/questionSet/question/dynamicAttributes/columnAttribute",
                ColumnAttributeField.class, "addAttributeField");

        configureLinkTextFields(digester,
                "wdkModel/questionSet/question/dynamicAttributes/");
    }

    private void configureXmlQuestionSet(Digester digester) {
        // load XmlQuestionSet
        configureNode(digester, "wdkModel/xmlQuestionSet",
                XmlQuestionSet.class, "addXmlQuestionSet");

        configureNode(digester, "wdkModel/xmlQuestionSet/description",
                WdkModelText.class, "addDescription");
        digester.addCallMethod("wdkModel/xmlQuestionSet/description",
                "setText", 0);

        // load XmlQuestion
        configureNode(digester, "wdkModel/xmlQuestionSet/xmlQuestion",
                XmlQuestion.class, "addQuestion");

        configureNode(digester,
                "wdkModel/xmlQuestionSet/xmlQuestion/description",
                WdkModelText.class, "addDescription");
        digester.addCallMethod(
                "wdkModel/xmlQuestionSet/xmlQuestion/description", "setText", 0);

        configureNode(digester, "wdkModel/xmlQuestionSet/xmlQuestion/help",
                WdkModelText.class, "addHelp");
        digester.addCallMethod("wdkModel/xmlQuestionSet/xmlQuestion/help",
                "setText", 0);
    }

    private void configureParamValuesSet(Digester digester, String path,
            String addMethodName) {
        digester.addObjectCreate(path, ParamValuesSet.class);
        digester.addSetProperties(path);
        digester.addCallMethod(path + "/paramValue", "put", 2);
        digester.addCallParam(path + "/paramValue", 0, "name");
        digester.addCallParam(path + "/paramValue", 1);
        digester.addSetNext(path, addMethodName);
    }

    private void configureXmlRecordClassSet(Digester digester) {
        // load XmlRecordClassSet
        configureNode(digester, "wdkModel/xmlRecordClassSet",
                XmlRecordClassSet.class, "addXmlRecordClassSet");

        // load XmlRecordClass
        configureNode(digester, "wdkModel/xmlRecordClassSet/xmlRecordClass",
                XmlRecordClass.class, "addRecordClass");

        // load XmlAttributeField
        configureNode(digester,
                "wdkModel/xmlRecordClassSet/xmlRecordClass/xmlAttribute",
                XmlAttributeField.class, "addAttributeField");

        // load XmlTableField
        configureNode(digester,
                "wdkModel/xmlRecordClassSet/xmlRecordClass/xmlTable",
                XmlTableField.class, "addTableField");

        // load XmlAttributeField within table
        configureNode(
                digester,
                "wdkModel/xmlRecordClassSet/xmlRecordClass/xmlTable/xmlAttribute",
                XmlAttributeField.class, "addAttributeField");
    }

    private void configureGroupSet(Digester digester) {
        // load GroupSet
        configureNode(digester, "wdkModel/groupSet", GroupSet.class,
                "addGroupSet");

        // load group
        configureNode(digester, "wdkModel/groupSet/group", Group.class,
                "addGroup");

        configureNode(digester, "wdkModel/groupSet/group/description",
                WdkModelText.class, "addDescription");
        digester.addCallMethod("wdkModel/groupSet/group/description",
                "setText", 0);
    }

    private void configureLinkTextFields(Digester digester, String prefix) {
        // link attribute
        configureNode(digester, prefix + "linkAttribute",
                LinkAttributeField.class, "addAttributeField");
        configureNode(digester, prefix + "linkAttribute/url",
                WdkModelText.class, "addUrl");
        digester.addCallMethod(prefix + "linkAttribute/url", "setText", 0);
        configureNode(digester, prefix + "linkAttribute/displayText",
                WdkModelText.class, "addDisplayText");
        digester.addCallMethod(prefix + "linkAttribute/displayText", "setText",
                0);

        // text attribute
        configureNode(digester, prefix + "textAttribute",
                TextAttributeField.class, "addAttributeField");

        configureNode(digester, prefix + "textAttribute/text",
                WdkModelText.class, "addText");
        digester.addCallMethod(prefix + "textAttribute/text", "setText", 0);

        configureNode(digester, prefix + "textAttribute/display",
                WdkModelText.class, "addDisplay");
        digester.addCallMethod(prefix + "textAttribute/display", "setText", 0);
    }

    private void configureQueryMonitor(Digester digester) {
        // load GroupSet
        configureNode(digester, "wdkModel/queryMonitor", QueryMonitor.class,
                "addQueryMonitor");

        configureNode(digester, "wdkModel/queryMonitor/ignoreSlowQueryRegex",
                WdkModelText.class, "addIgnoreSlowQueryRegex");
        digester.addCallMethod("wdkModel/queryMonitor/ignoreSlowQueryRegex",
                "setText", 0);

        configureNode(digester, "wdkModel/queryMonitor/ignoreBrokenQueryRegex",
                WdkModelText.class, "addIgnoreBrokenQueryRegex");
        digester.addCallMethod("wdkModel/queryMonitor/ignoreBrokenQueryRegex",
                "setText", 0);
    }

    public static void main(String[] args) throws SAXException, IOException,
            ParserConfigurationException, TransformerFactoryConfigurationError,
            TransformerException, WdkModelException, NoSuchAlgorithmException,
            SQLException, JSONException, WdkUserException,
            InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        String cmdName = System.getProperty("cmdName");

        // process args
        Options options = declareOptions();
        CommandLine cmdLine = parseOptions(cmdName, options, args);
        String projectId = cmdLine.getOptionValue(Utilities.ARGUMENT_PROJECT_ID);
        String gusHome = System.getProperty(Utilities.SYSTEM_PROPERTY_GUS_HOME);

        // create a parser, and parse the model file
        WdkModel wdkModel = WdkModel.construct(projectId, gusHome);

        // print out the model content
        System.out.println(wdkModel.toString());
        System.exit(0);
    }

    private static void addOption(Options options, String argName, String desc) {

        Option option = new Option(argName, true, desc);
        option.setRequired(true);
        option.setArgName(argName);

        options.addOption(option);
    }

    private static Options declareOptions() {
        Options options = new Options();

        // config file
        addOption(options, "model", "the name of the model.  This is used to "
                + "find the Model XML file ($GUS_HOME/lib/wdk/model_name.xml) "
                + "the Model property file ($GUS_HOME/config/model_name.prop) "
                + "and the Model config file "
                + "($GUS_HOME/config/model_name-config.xml)");

        return options;
    }

    private static CommandLine parseOptions(String cmdName, Options options,
            String[] args) {

        CommandLineParser parser = new BasicParser();
        CommandLine cmdLine = null;
        try {
            // parse the command line arguments
            cmdLine = parser.parse(options, args);
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("");
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            System.err.println("");
            usage(cmdName, options);
        }

        return cmdLine;
    }

    private static void usage(String cmdName, Options options) {

        String newline = System.getProperty("line.separator");
        String cmdlineSyntax = cmdName + " -model model_name";

        String header = newline + "Parse and print out a WDK Model xml file."
                + newline + newline + "Options:";

        String footer = "";

        // PrintWriter stderr = new PrintWriter(System.err);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(75, cmdlineSyntax, header, options, footer);
        System.exit(1);
    }
}
