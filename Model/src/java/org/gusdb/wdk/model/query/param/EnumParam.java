package org.gusdb.wdk.model.query.param;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.jspwrap.EnumParamCache;
import org.gusdb.wdk.model.user.User;
import org.json.JSONObject;

/**
 * The enumParam represents a list of param values declared in the model that user can choose from.
 * 
 * @author jerric
 * 
 */
public class EnumParam extends AbstractEnumParam {

  private List<EnumItemList> enumItemLists;
  private EnumItemList enumItemList;

  public EnumParam() {
    enumItemLists = new ArrayList<EnumItemList>();
  }

  public EnumParam(EnumParam param) {
    super(param);
    this.enumItemList = param.enumItemList;
  }

  // ///////////////////////////////////////////////////////////////////
  // /////////// Public properties ////////////////////////////////////
  // ///////////////////////////////////////////////////////////////////

  public void addEnumItemList(EnumItemList enumItemList) {
    this.enumItemLists.add(enumItemList);
  }

  // ///////////////////////////////////////////////////////////////////
  // /////////// Protected properties ////////////////////////////////////
  // ///////////////////////////////////////////////////////////////////

  @Override
  protected EnumParamCache createEnumParamCache(User user, Map<String, String> dependedParamValues)
      throws WdkModelException {
    logger.trace("Entering createEnumParamCache(" + FormatUtil.prettyPrint(dependedParamValues) + ")");
    Set<Param> dependedParams = getDependedParams();
    EnumParamCache cache = new EnumParamCache(this, dependedParamValues);
    EnumItem[] enumItems = enumItemList.getEnumItems();
    for (EnumItem item : enumItems) {
      String term = item.getTerm();
      String display = item.getDisplay();
      String parentTerm = item.getParentTerm();
      boolean skip = false;

      // escape the term & parentTerm
      // term = term.replaceAll("[,]", "_");
      // if (parentTerm != null)
      // parentTerm = parentTerm.replaceAll("[,]", "_");
      if (term.indexOf(',') >= 0 && dependedParams != null)
        throw new WdkModelException(this.getFullName() + ": The term cannot contain comma: '" + term + "'");
      if (parentTerm != null && parentTerm.indexOf(',') >= 0)
        throw new WdkModelException(this.getFullName() + ": The parent term cannot contain" + "comma: '" +
            parentTerm + "'");

      if (isDependentParam()) {
        // if this is a dependent param, only include items that are
        // valid for the current depended value
        skip = !item.isValidFor(dependedParamValues);
      }

      if (!skip) {
        cache.addTermValues(term, item.getInternal(), display, parentTerm);
      }
    }
    // check if the result is empty
    if (cache.isEmpty())
      throw new WdkEmptyEnumListException("The EnumParam [" + getFullName() + "] doesn't have any values.");

    initTreeMap(cache);
    applySelectMode(cache);
    logger.trace("Leaving createEnumParamCache(" + FormatUtil.prettyPrint(dependedParamValues) + ")");
    return cache;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.Param#excludeResources(java.lang.String)
   */
  @Override
  public void excludeResources(String projectId) throws WdkModelException {
    super.excludeResources(projectId);

    // exclude enum items
    boolean hasEnumList = false;
    for (EnumItemList itemList : enumItemLists) {
      if (itemList.include(projectId)) {
        if (hasEnumList) {
          throw new WdkModelException("enumParam " + getFullName() +
              " has more than one <enumList> for project " + projectId);
        }
        else {
          EnumItem[] enumItems = itemList.getEnumItems();
          if (enumItems.length == 0)
            throw new WdkModelException("enumParam '" + this.name + "' has zero items");

          itemList.setParam(this);
          itemList.excludeResources(projectId);
          this.enumItemList = itemList;

          hasEnumList = true;
        }
      }
    }
    enumItemLists = null;
    if (enumItemList == null || enumItemList.getEnumItems().length == 0)
      throw new WdkModelException("No enum items available in enumParam " + getFullName());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.Param#resolveReferences(org.gusdb.wdk.model.WdkModel)
   */
  @Override
  public void resolveReferences(WdkModel model) throws WdkModelException {
    super.resolveReferences(model);

    enumItemList.resolveReferences(model);

    StringBuffer sb = new StringBuffer();
    EnumItem[] enumItems = enumItemList.getEnumItems();
    for (EnumItem item : enumItems) {
      if (item.isDefault()) {
        if (sb.length() > 0) {
          // single pick default should be singular value
          if (!multiPick)
            break;
          sb.append(",");
        }
        sb.append(item.getTerm());
      }
    }
    if (sb.length() > 0) {
      setDefault(sb.toString());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.Param#clone()
   */
  @Override
  public Param clone() {
    return new EnumParam(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.gusdb.wdk.model.Param#appendJSONContent(org.json.JSONObject)
   */
  @Override
  protected void appendJSONContent(JSONObject jsParam, boolean extra) {
    // do nothing. do not add the enum list into the content, since they may
    // be
    // changed between versions, but we don't want to invalidate a query
    // because
    // of it.
  }
}