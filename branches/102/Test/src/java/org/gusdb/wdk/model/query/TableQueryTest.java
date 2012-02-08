/**
 * 
 */
package org.gusdb.wdk.model.query;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.wdk.model.RecordClass;
import org.gusdb.wdk.model.RecordClassSet;
import org.gusdb.wdk.model.TableField;
import org.gusdb.wdk.model.UnitTestHelper;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.query.param.ParamValuesSet;
import org.gusdb.wdk.model.user.User;
import org.json.JSONException;
import org.junit.Test;

/**
 * @author xingao
 * 
 */
public class TableQueryTest {

    private User user;
    private WdkModel wdkModel;

    public TableQueryTest() throws Exception {
        this.user = UnitTestHelper.getRegisteredUser();
        this.wdkModel = UnitTestHelper.getModel();
    }

    @Test
    public void testTableQueries() throws NoSuchAlgorithmException,
            WdkModelException, SQLException, JSONException, WdkUserException {
        for (RecordClassSet recordClassSet : wdkModel.getAllRecordClassSets()) {
            for (RecordClass recordClass : recordClassSet.getRecordClasses()) {
                for (TableField table : recordClass.getTableFields()) {
                    Query query = table.getQuery();
                    if (query.getDoNotTest())
                        continue;
                    for (ParamValuesSet valueSet : query.getParamValuesSets()) {
                        Map<String, String> values = valueSet.getParamValues();
                        if (values.size() == 0)
                            continue;
                        int min = valueSet.getMinRows();
                        int max = valueSet.getMaxRows();
                        QueryInstance instance = query.makeInstance(user,
                                values, true, 0,
                                new LinkedHashMap<String, String>());
                        int result = instance.getResultSize();
                        // Assert.assertTrue(result + " >= " + min, result >=
                        // min);
                        // Assert.assertTrue(result + " <= " + max, result <=
                        // max);
                    }
                }

            }
        }
    }
}
