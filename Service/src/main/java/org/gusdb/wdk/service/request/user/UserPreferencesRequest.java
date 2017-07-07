package org.gusdb.wdk.service.request.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gusdb.fgputil.iterator.IteratorUtil;
import org.gusdb.wdk.service.formatter.Keys;
import org.gusdb.wdk.service.request.exception.RequestMisformatException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parses the JSON object returned by either a PUT or PATCH REST request for
 * User preferences
 * @author crisl-adm
 *
 */
public class UserPreferencesRequest {

  private Map<String,String> _globalPrefs = new HashMap<>();
  private List<String> _globalPrefsToDelete = new ArrayList<>();
  private Map<String,String> _projectPrefs = new HashMap<>();
  private List<String> _projectPrefsToDelete = new ArrayList<>();

  /**
   * Input Format:
   * {
   *   global:  { String key: String },
   *   project: { String key: String }
   * }
   * 
   * @param json
   * @return
   * @throws RequestMisformatException
   */
  public static UserPreferencesRequest createFromJson(JSONObject json) throws RequestMisformatException {
    try {
      UserPreferencesRequest request = new UserPreferencesRequest();
      loadPreferenceChanges(json, Keys.GLOBAL, request._globalPrefs, request._globalPrefsToDelete);
      loadPreferenceChanges(json, Keys.PROJECT, request._projectPrefs, request._projectPrefsToDelete);
      return request;
    }
    catch (JSONException e) {
      String detailMessage = e.getMessage() != null ? e.getMessage() : "No additional information.";
      throw new RequestMisformatException(detailMessage, e);
    }
  }

  private static void loadPreferenceChanges(JSONObject parent, String objectKey,
      Map<String,String> prefChanges, List<String> prefDeletes) {
    if (parent.has(objectKey)) {
      JSONObject prefs = parent.getJSONObject(objectKey);
      for (String key : IteratorUtil.toIterable((Iterator<String>)prefs.keys())) {
        if (prefs.isNull(key)) {
          prefDeletes.add(key);
        }
        else {
          prefChanges.put(key, prefs.getString(key));
        }
      }
    }
  }

  public Map<String,String> getGlobalPreferenceMods() {
    return _globalPrefs;
  }

  public List<String> getGlobalPreferenceDeletes() {
    return _globalPrefsToDelete;
  }

  public Map<String,String> getProjectPreferenceMods() {
    return _projectPrefs;
  }

  public List<String> getProjectPreferenceDeletes() {
    return _projectPrefsToDelete;
  }
}
