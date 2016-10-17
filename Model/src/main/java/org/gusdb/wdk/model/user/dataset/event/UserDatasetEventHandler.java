package org.gusdb.wdk.model.user.dataset.event;

import java.nio.file.Path;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.dataset.UserDataset;
import org.gusdb.wdk.model.user.dataset.UserDatasetStore;
import org.gusdb.wdk.model.user.dataset.UserDatasetTypeHandler;
import org.gusdb.wdk.model.user.dataset.event.UserDatasetAccessControlEvent.AccessControlAction;

public class UserDatasetEventHandler {

  public static void handleInstallEvent (UserDatasetInstallEvent event, UserDatasetTypeHandler typeHandler, UserDatasetStore userDatasetStore, DataSource appDbDataSource, String userDatasetSchemaName, Path tmpDir) throws WdkModelException {
    String sql = "insert into " + userDatasetSchemaName + ".InstalledUserDataset (user_dataset_id) values (?)";

    SQLRunner sqlRunner = new SQLRunner(appDbDataSource, sql);
    Object[] args = {event.getUserDatasetId()};
    sqlRunner.executeUpdate(args);

    UserDataset userDataset = userDatasetStore.getUserDataset(event.getOwnerUserId(), event.getUserDatasetId());
    typeHandler.installInAppDb(userDataset, tmpDir);
  }
  
  public static void handleUninstallEvent (UserDatasetUninstallEvent event, UserDatasetTypeHandler typeHandler, DataSource appDbDataSource, String userDatasetSchemaName) {
    typeHandler.uninstallInAppDb(event.getUserDatasetId(), appDbDataSource, userDatasetSchemaName);
    String sql = "delete from " + userDatasetSchemaName + ".InstalledUserDataset where dataset_id = ?";

    SQLRunner sqlRunner = new SQLRunner(appDbDataSource, sql);
    Object[] args = {event.getUserDatasetId()};
    sqlRunner.executeUpdate(args);

  }
  
  public static void handleAccessControlEvent (UserDatasetAccessControlEvent event, DataSource appDbDataSource, String userDatasetSchemaName) {
    String sql;
    if (event.getAction() == AccessControlAction.GRANT) {
      sql = "insert into " + userDatasetSchemaName + ".UserDatasetAccessControl (user_id, user_dataset_id) values (?, ?)";
    } else {  // REVOKE
      sql = "delete from " + userDatasetSchemaName + ".UserDatasetAccessControl where user_id = ? and dataset_id = ?";
    }
    SQLRunner sqlRunner = new SQLRunner(appDbDataSource, sql);
    Object[] args = {event.getUserId(), event.getUserDatasetId()};
    sqlRunner.executeUpdate(args);
  }
  
}
