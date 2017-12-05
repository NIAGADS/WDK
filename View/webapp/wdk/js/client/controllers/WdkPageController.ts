import AbstractPageController from './AbstractPageController';
import WdkStore, { BaseState } from '../stores/WdkStore';
import { ActionCreator, ActionCreatorRecord } from '../utils/ActionCreatorUtils';
import { Action } from '../dispatcher/Dispatcher';

/**
 * Simple implementation of 'AbstractPageController' that uses 'WdkStore' and
 * its state. This is useful for PageControllers that do not need anything in
 * addition to 'BaseState'.
 *
 * This is here mostly because it is not possible to provide this default
 * functionality in AbstractPageController. The type system is unable to ensure
 * that BaseState is compatible with the AbstractPageController's generic State
 * type parameter. This could potentially be avoided by removing the
 * 'getStateFromStore' method hook, but the easy-to-add performance gains we
 * get from 'getStateFromStore' might outweigh the extra layer here.
 */
export default class WdkPageController<ActionCreators extends ActionCreatorRecord<Action> = {}> extends AbstractPageController<BaseState, WdkStore, ActionCreators> {

  getStateFromStore() {
    return this.store.getState();
  }

  getStoreClass() {
    return WdkStore;
  }

}
