package org.gusdb.wdk.events;

import org.gusdb.fgputil.events.Event;
import org.gusdb.wdk.model.user.Step;

/**
 * This event will be fired when one step is copied to another step.  Any step-
 * dependent objects should listen for this event and react as necessary.
 * 
 * @author ryan
 */
public class StepCopiedEvent extends Event {

  private final Step _fromStep, _toStep;

  public StepCopiedEvent(Step fromStep, Step toStep) {
    _fromStep = fromStep;
    _toStep = toStep;
  }

  public Step getFromStep() {
    return _fromStep;
  }

  public Step getToStep() {
    return _toStep;
  }
}
