package org.gusdb.wdk.jmx.mbeans;

/**
 * A view of the WDK's representation of model-config.xml.
 * The configuration data comes from WDK class instances, not directly 
 * from configuation files on the filesystem, so it's important to note
 * that the WDK may have added or removed or even changed some values
 * relative to the state on the filesystem.
 *
 * @see org.gusdb.wdk.jmx.mbeans.AbstractConfig#setValuesFromGetters
 * @see org.gusdb.wdk.model.ModelConfig
 * @see org.gusdb.wdk.model.config.ModelConfigUserDB
 * @see org.gusdb.wdk.model.config.ModelConfigAppDB
 */
public class ModelConfig extends AbstractAttributesBean {

  public ModelConfig() {
    init();
  }

  @Override
  protected void init() {
    org.gusdb.wdk.model.config.ModelConfig          modelConfig          = getWdkModel().getModelConfig();
    org.gusdb.wdk.model.config.QueryMonitor         queryMonitor         = modelConfig.getQueryMonitor();
    org.gusdb.wdk.model.config.ModelConfigUserDB    modelConfigUserDB    = modelConfig.getUserDB();
    org.gusdb.wdk.model.config.ModelConfigAppDB     modelConfigAppDB     = modelConfig.getAppDB();
    org.gusdb.wdk.model.config.ModelConfigAppDB     modelConfigAnnotationDB     = modelConfig.getAnnotationDB();
    org.gusdb.wdk.model.config.ModelConfigAccountDB modelConfigAccountDB = modelConfig.getAccountDB();
    org.gusdb.wdk.model.config.ModelConfigUserDatasetStore
      modelConfigUserDatasetStore  = modelConfig.getUserDatasetStoreConfig();

    setValuesFromGetters("global", modelConfig);
    setValuesFromGetters("queryMonitor", queryMonitor);
    setValuesFromGetters("userDb", modelConfigUserDB);
    setValuesFromGetters("appDb",  modelConfigAppDB);
    setValuesFromGetters("annotationDb",  modelConfigAnnotationDB);
    setValuesFromGetters("accountDb", modelConfigAccountDB);
    setValuesFromGetters("userDatasetStore",  modelConfigUserDatasetStore);
  }

}
