package org.gusdb.wdk.model.user.dataset.event;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.runner.BasicResultSetHandler;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.user.dataset.UserDataset;
import org.gusdb.wdk.model.user.dataset.UserDatasetSession;
import org.gusdb.wdk.model.user.dataset.UserDatasetTypeHandler;
import org.gusdb.wdk.model.user.dataset.event.UserDatasetShareEvent.ShareAction;

/**
 * Handle events that impact which user datasets a user can use in this website.
 *  We use the word "installed" to mean that a user dataset is available for use
 * on this website for this user.  It never means anything else.
 * <p>
 * Three database tables control if a user sees a dataset as installed. 1) The
 * InstalledUserDataset table holds the IDs of all datasets that are installed
 * for use on this site. It includes the name of the UD, to show to the user in
 * parameters in WDK Searches. 2) the UserDatasetOwner table tells us who owns
 * the UD that is in the InstalledUserDataset table. It has a foreign key to the
 * InstalledUserDataset table. 3) the UserDatasetSharedWith table tells us who
 * has share access to an installed UD.  has a foreign key to the
 * InstalledUserDataset table.
 * <p>
 * An install event causes the UD to be inserted into the install table and the
 * owner table.
 * <p>
 * A share event causes the a row to be inserted into the shared table, (and
 * unshare is vice versa)
 * <p>
 * A delete event causes rows from share, owner and install table to be
 * removed.
 * <p>
 * To see which UDs a user has installed, we query the union of the Owner and
 * Shared table.
 * <p>
 * TODO: it seems we should add the owner as a column to the
 *   InstalledUserDatasets table, and lose the Owner table, since they are 1-1
 * <p>
 * TODO: if the user changes the name of their UD, this will not be reflected in
 *   installed UDs, since there is no event to convey that.
 *
 * @author Steve
 */
public class UserDatasetEventHandler {

  private static final Logger LOG = Logger.getLogger(UserDatasetEventHandler.class);

  private static final String
    installedTable = "InstalledUserDataset",
    ownerTable = "UserDatasetOwner",
    sharedTable = "UserDatasetSharedWith",
    eventTable = "UserDatasetEvent";

  private static final int copyTimeoutMinutes = 180;

  private final UserDatasetSession dsSession;
  private final DataSource dataSource;
  private final Path tmpDir;
  private final String dsSchema;
  private final String projectId;

  public UserDatasetEventHandler(
    final UserDatasetSession dsSession,
    final DataSource dataSource,
    final Path tmpDir,
    final String dsSchema,
    final String projectId
  ) {
    this.dataSource = dataSource;
    this.tmpDir = tmpDir;
    this.dsSchema = dsSchema;
    this.projectId = projectId;
    this.dsSession = dsSession;
  }

  public void handleInstallEvent (
    UserDatasetInstallEvent event,
    UserDatasetTypeHandler typeHandler
  ) throws WdkModelException {
    final var datasetId = event.getUserDatasetId();
    final var eventId = event.getEventId();

    LOG.info("Installing user dataset " + datasetId);

    openEventHandling(eventId);
    handleInstallEvent(typeHandler, event.getOwnerUserId(), datasetId);
    closeEventHandling(eventId);
  }

  public void handleUninstallEvent (
    UserDatasetUninstallEvent event,
    UserDatasetTypeHandler typeHandler
  ) throws WdkModelException {

    LOG.info("Uninstalling user dataset " + event.getUserDatasetId());
    openEventHandling(event.getEventId());

    revokeAllAccess(event.getUserDatasetId());
    typeHandler.uninstallInAppDb(event.getUserDatasetId(), tmpDir, projectId);
    var sql = "delete from " + dsSchema + installedTable + " where user_dataset_id = ?";

    var sqlRunner = new SQLRunner(dataSource, sql, "delete-user-dataset-row");
    sqlRunner.executeUpdate(new Object[]{event.getUserDatasetId()});
    closeEventHandling(event.getEventId());
  }

  public void handleShareEvent(UserDatasetShareEvent event) {

    LOG.info("Updating share of user dataset " + event.getUserDatasetId() );
    openEventHandling(event.getEventId());

    if (!checkUserDatasetInstalled(event.getUserDatasetId())) {
      // this can happen if the install was skipped, because the ud was deleted first
      LOG.info("User dataset " + event.getUserDatasetId() + " is not installed. Skipping share.");
    } else {
      if (event.getAction() == ShareAction.GRANT)
        grantShareAccess(event.getOwnerId(), event.getRecipientId(),
          event.getUserDatasetId());
      else
        revokeShareAccess(event.getOwnerId(), event.getRecipientId(),
          event.getUserDatasetId());
    }
    closeEventHandling(event.getEventId());
  }

  /**
   * Method to handle an event that is either not relevant to this wdk project
   * or is related to an unsupported type.  The event is not installed but it is
   * noted in the dataset as handled so that the event is not repeatedly and
   * unnecessarily processed.
   */
  public void completeEventHandling(Long eventId) {
    openEventHandling(eventId);
    closeEventHandling(eventId);
  }

  private void handleInstallEvent(
    final UserDatasetTypeHandler typeHandler,
    final Long ownerUserId,
    final Long datasetId
  ) throws WdkModelException {
    LOG.trace("UserDatasetEventHandler#handleInstallEvent");
    final Path cwd;
    final UserDataset userDataset;
    final Map<String, Path> files;

    // there is a theoretical race condition here, because this check is not
    // in the same transaction as the rest of this method.
    // but that risk is very small.
    if (!dsSession.getUserDatasetExists(ownerUserId, datasetId)) {
      LOG.info("User dataset" + datasetId + " not found in store.  Was probably deleted.  Skipping install.");
      return;
    }

    userDataset = dsSession.getUserDataset(ownerUserId, datasetId);

    // Weeding out obsolete user datasets - skipped but completed.
    var compatibility = typeHandler.getCompatibility(userDataset, dataSource);

    if (!compatibility.isCompatible()) {
      LOG.info("User dataset " + datasetId + " deemed obsolete: " + compatibility.notCompatibleReason() + ".  Skipping install.");
      return;
    }

    cwd = typeHandler.createWorkingDir(tmpDir, userDataset.getUserDatasetId());
    files = copyToLocalTimeout(dsSession, userDataset, typeHandler, cwd);

    // insert into the installedTable
    var sql = "insert into " + dsSchema + installedTable +
      " (user_dataset_id, name) values (?, ?)";

    new SQLRunner(dataSource, sql, "insert-user-dataset-row")
      .executeUpdate(new Object[]{
        datasetId,
        userDataset.getMeta().getName()
      });

    // insert into the type-specific tables
    typeHandler.installInAppDb(userDataset, cwd, projectId, files);
    typeHandler.deleteWorkingDir(cwd);

    // grant access to the owner, by installing into the ownerTable
    grantAccess(ownerUserId, datasetId);
  }

  /**
   * Copies the contents of a user dataset from iRODS to a temporary working dir
   * under the given path {@code tmpDir} or fails if the copy takes longer than
   * {@link #copyTimeoutMinutes}.
   *
   * @param session active iRODS session
   * @param dataset user dataset from which the files should be copied.
   * @param typeHandler dataset type handler.  Used to create the working
   *        directory and perform the file copy.
   * @param cwd working directory path.  All relevant dataset files will be
   *        copied from iRODS to this directory.
   *
   * @return a map of dataset file names to their path in the filesystem.
   *
   * @throws WdkModelException if an error occurs during the file copy, or if
   *         the copy request times out.
   */
  private Map<String, Path> copyToLocalTimeout(
    final UserDatasetSession session,
    final UserDataset dataset,
    final UserDatasetTypeHandler typeHandler,
    final Path cwd
  ) throws WdkModelException {
    LOG.trace("UserDatasetEventHandler#copyToLocalTimeout");

    final var exec =  Executors.newSingleThreadExecutor();
    final var err = new AtomicReference<WdkModelException>();
    final var out = new AtomicReference<Map<String, Path>>();

    exec.execute(() -> {
      try {
        out.set(typeHandler.copyFilesToTemp(session, dataset, cwd));
      } catch (WdkModelException e) {
        err.set(e);
      }
    });

    exec.shutdown();
    try {
      if (!exec.awaitTermination(copyTimeoutMinutes, TimeUnit.MINUTES)) {
        throw new WdkModelException("Copy from iRODS to temp directory timed out after " + copyTimeoutMinutes + " minutes.");
      }
    } catch (InterruptedException e) {
      throw new WdkModelException(e);
    }

    if (err.get() != null)
      throw err.get();

    return out.get();
  }

  /**
   * check if a user dataset is installed (in the installed table). operations
   * that call this method are at theoretical risk of a race condition, since
   * this check is in its own transaction.  but the chance that a ud will be
   * uninstalled in the intervening millisecond is small, not worth engineering
   * for.
   */
  private boolean checkUserDatasetInstalled(Long userDatasetId) {
    LOG.info("Checking if user dataset " + userDatasetId + " is installed");
    var handler = new BasicResultSetHandler();
    var sql = "SELECT user_dataset_id"
      + " FROM " + dsSchema + installedTable
      + " WHERE user_dataset_id = ?";

    new SQLRunner(dataSource, sql, "check-user-dataset-exists")
      .executeQuery(new Object[]{userDatasetId}, handler);

    return handler.getNumRows() > 0;
  }

  /**
   * Adds a share to the UserDatasetSharedWith table.
   */
  private void grantShareAccess(Long ownerId, Long recipientId, Long userDatasetId) {
    LOG.info("Granting recipient " + recipientId + " access to user dataset " + userDatasetId + " belonging to owner " + ownerId + " in table " + sharedTable);
    var sql = "INSERT INTO " + dsSchema + sharedTable + " (owner_user_id, recipient_user_id, user_dataset_id) VALUES (?, ?, ?)";
    var sqlRunner = new SQLRunner(dataSource, sql, "grant-user-dataset-" + sharedTable);

    sqlRunner.executeUpdate(new Object[]{ownerId, recipientId, userDatasetId});
  }

  private void grantAccess(Long userId, Long userDatasetId) {
    LOG.info("Granting access to user dataset " + userDatasetId + " to user " + userId + " in table " + ownerTable);
    var sql = "INSERT INTO " + dsSchema + ownerTable + " (user_id, user_dataset_id) VALUES (?, ?)";
    var sqlRunner = new SQLRunner(dataSource, sql, "grant-user-dataset-" + ownerTable);

    sqlRunner.executeUpdate(new Object[]{userId, userDatasetId});
  }

  private void revokeShareAccess(
    Long ownerId,
    Long recipientId,
    Long userDatasetId
  ) {
    LOG.info("Revoking access by recipient " + recipientId + " to user dataset " + userDatasetId + " belonging to owner " + ownerId);
    var sql = "DELETE FROM " + dsSchema + sharedTable
      + " WHERE owner_user_id = ?"
      + " AND recipient_user_id = ?"
      + " AND user_dataset_id = ?";
    var sqlRunner = new SQLRunner(dataSource, sql, "revoke-user-dataset-" + sharedTable);

    sqlRunner.executeUpdate(new Object[]{ownerId, recipientId, userDatasetId});
  }

  private void revokeAllAccess(Long userDatasetId) {
    LOG.info("Revoking all access to user dataset " + userDatasetId);
    var args = new Object[]{userDatasetId};
    var sql = "DELETE FROM " + dsSchema + ownerTable + " WHERE user_dataset_id = ?";
    var sqlRunner = new SQLRunner(dataSource, sql, "revoke-all-user-dataset-access-1");

    sqlRunner.executeUpdate(args);

    sql = "DELETE FROM " + dsSchema + sharedTable + " WHERE user_dataset_id = ?";
    sqlRunner = new SQLRunner(dataSource, sql, "revoke-all-user-dataset-access-1");
    sqlRunner.executeUpdate(args);
  }

  private void openEventHandling(Long eventId) {
    LOG.info("Start handling event: " + eventId);
    var sql = "INSERT INTO " + dsSchema + eventTable + " (event_id) VALUES (?)";
    var sqlRunner = new SQLRunner(dataSource, sql, "insert-user-dataset-event");
    sqlRunner.executeUpdate(new Object[]{eventId});
  }

  private void closeEventHandling(Long eventId) {
    var sql = "UPDATE " + dsSchema + eventTable
      + " SET completed = sysdate"
      + " WHERE event_id = ?";
    var sqlRunner = new SQLRunner(dataSource, sql, "complete-user-dataset-event-handling");
    sqlRunner.executeUpdate(new Object[]{eventId});

    LOG.info("Done handling event: " + eventId);
  }
}
