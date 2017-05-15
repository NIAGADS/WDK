import React from 'react';
import { PropTypes } from 'react';
import TextBox from './TextBox';
import { wrappable } from '../utils/componentUtils';

/**
 * This React stateless function displays the user identification fieldset of the form.
 * @param props
 * @returns {XML}
 * @constructor
 */
const UserIdentity = (props) => {
  let { user, onPropertyChange } = props;
  return (
    <fieldset>
      <legend>Identification</legend>
      <div>
        <label htmlFor="userEmail"><i className="fa fa-asterisk"></i>Email:</label>
        <TextBox type="email" id="userEmail" value={user.email} required="required"
            onChange={props.onEmailChange} maxLength="255" size="80"
            placeholder="Your email is used as your unique user id" />
      </div>
      <div>
        <label htmlFor="confirmUserEmail"><i className="fa fa-asterisk"></i>Retype Email:</label>
        <TextBox type="email" id="confirmUserEmail" value={user.confirmEmail} required="required"
            onChange={props.onConfirmEmailChange} maxLength="255" size="80"
            placeholder="Your email is used as your unique user id" />
      </div>
      {props.propDefs.map(propDef => {
        let { name, displayName, isRequired } = propDef;
        return (
          <div>
            <label htmlFor="{name}">{ isRequired ? <i className="fa fa-asterisk"></i> : ''}{displayName}:</label>
            <TextBox id="{name}" value={user[name]} required={isRequired}
                onChange={onPropertyChange(name)} maxLength="255" size="80" />
          </div>
        );
      })}
    </fieldset>
  );
};


UserIdentity.propTypes = {

  /** The user object to be modified */
  user:  PropTypes.object.isRequired,

  /** The on change handler for email text box input */
  onEmailChange:  PropTypes.func.isRequired,

  /** The on change handler for confirm email text box input */
  onConfirmEmailChange: PropTypes.func.isRequired,

  /** The on change handler for user profile properties inputs */
  onPropertyChange:  PropTypes.func.isRequired,

  /** An array of the user properties configured in WDK model */
  propDefs: PropTypes.array.isRequired

};

export default wrappable(UserIdentity);
