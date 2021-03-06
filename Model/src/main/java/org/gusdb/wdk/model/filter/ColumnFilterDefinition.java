package org.gusdb.wdk.model.filter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.attribute.QueryColumnAttributeField;

public class ColumnFilterDefinition extends FilterDefinition {

  private Class<? extends ColumnFilter> _class;

  @Override
  public void resolveReferences(WdkModel wdkModel) throws WdkModelException {
    super.resolveReferences(wdkModel);

    try {
      _class = Class.forName(getImplementation()).asSubclass(ColumnFilter.class);
    }
    catch (ClassNotFoundException | ClassCastException ex) {
      throw new WdkModelException(ex);
    }
  }

  public ColumnFilter getColumnFilter(QueryColumnAttributeField columnAttribute) throws WdkModelException {
    try {
      Constructor<? extends ColumnFilter> constructor = _class.getConstructor(String.class, QueryColumnAttributeField.class);
      ColumnFilter columnFilter = constructor.newInstance(getName(), columnAttribute);
      initializeFilter(columnFilter);
      return columnFilter;
    }
    catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException ex) {
      throw new WdkModelException(ex);
    }
  }
}
