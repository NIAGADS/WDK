import {Link as RRLink} from 'react-router';
import {wrappable} from '../utils/componentUtils';

/** React Router Link decorator that adds className */
function Link(props) {
  return (
    <RRLink {...props} className={'wdk-ReactRouterLink ' + props.className}/>
  );
}

export default wrappable(Link);
