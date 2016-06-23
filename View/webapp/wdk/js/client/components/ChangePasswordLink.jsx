import { PropTypes } from 'react';
import { wrappable } from '../utils/componentUtils';
import Link from './Link';

/**
 * Provides a link to the password change form- either one configured in the
 * WDK model or the WDK default change password form.
 * 
 * @param props
 */
const ChangePasswordLink = (props) => {

  if (props.changePasswordUrl != null && props.changePasswordUrl != "") {
    // Use prop URL and add optional query params to help it out
    // Expect something like: changePassword.html?returnUrl={{returnUrl}}&suggestedUsername={{suggestedUsername}}
    let url = props.changePasswordUrl
        .replace('{{returnUrl}}', encodeURIComponent(window.location))
        .replace('{{suggestedUsername}}', encodeURIComponent(props.userEmail));
    return ( <a href={url}>{props.children}</a> );
  }
  else {
    // use default WDK change password page
    return ( <Link to={`/user/profile/password`}>{props.children}</Link> );
  }
}

ChangePasswordLink.propTypes = {

  /** The user object to be modified */
  userEmail:  PropTypes.string.isRequired,

  /** URL configured in WDK to change password (empty string will be treated as null/undefined) */
  changePasswordUrl:  PropTypes.string
};

export default wrappable(ChangePasswordLink);
