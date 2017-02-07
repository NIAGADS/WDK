import React, { Component, PropTypes } from 'react';
import {debounce} from 'lodash';
import Tooltip from './Tooltip';

/** classNames used by component */
const baseClassName = 'wdk-RealTimeSearchBox';
const inputClassName = baseClassName + 'Input';
const labelClassName = baseClassName + 'Label';
const cancelBtnClassName = baseClassName + 'CancelBtn';
const infoIconClassName = baseClassName + 'InfoIcon';
const cancelIconClassName = baseClassName + 'CancelIcon';
const searchIconClassName = baseClassName + 'SearchIcon';

type Props = {

  /** Class name to include with default class name */
  className?: string;

  /** Set the autofocus property of the underlying HTMLTextInputElement */
  autoFocus?: boolean;

  /** Initial search text; defaults to ''.  After mounting, search text is maintained by the component */
  searchTerm?: string;

  /** Called when user alters text in the search box  */
  onSearchTermChange?: (searchTerm: string) => void;

  /** The placeholder string if no search text is present; defaults to ''. */
  placeholderText?: string;

  /** Text to appear as tooltip of help icon, should describe how the search is performed. Defaults to empty (no icon) */
  helpText?: string;

  /** Delay in milliseconds after last character typed until onSearchTermChange is called.  Defaults to 250. */
  delayMs?: number;

}

type State = {

  /** local reference to search term for rendering */
  searchTerm: string;
}

/**
 * A 'real time' search box.  Changes are throttled by 'debounce' so text
 * change events are delayed to prevent repetitive costly searching.  Useful
 * when expensive operations are performed (e.g. search) in real time as the
 * user types in the box.  Also provides reset button to clear the box.
 */
export default class RealTimeSearchBox extends Component<Props, State> {


  static defaultProps = {
    className: '',
    autoFocus: false,
    searchTerm: '',
    onSearchTermChange: () => {},
    placeholderText: '',
    helpText: '',
    delayMs: 250
  };

  debounceOnSearchTermSet = debounce(this.props.onSearchTermChange!, this.props.delayMs);

  input: HTMLInputElement;

  constructor(props: Props) {
    super(props);
    this.handleSearchTermChange = this.handleSearchTermChange.bind(this);
    this.handleResetClick = this.handleResetClick.bind(this);
    this.handleKeyDown = this.handleKeyDown.bind(this);
    this.state = { searchTerm: this.props.searchTerm! };
  }

  componentDidMount() {
    if (this.props.autoFocus) this.input.autofocus = true;
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.onSearchTermChange !== this.props.onSearchTermChange)
      this.debounceOnSearchTermSet = debounce(nextProps.onSearchTermChange!, nextProps.delayMs);

    if (nextProps.searchTerm !== this.state.searchTerm) {
      this.debounceOnSearchTermSet(nextProps.searchTerm!);
      this.setState({ searchTerm: nextProps.searchTerm! });
    }
  }

  /**
   * Update the state of this Component, and call debounced onSearchTermSet
   * callback.
   */
  handleSearchTermChange(e: React.KeyboardEvent<HTMLInputElement>) {
    let searchTerm = e.currentTarget.value;
    this.setState({ searchTerm });
    this.debounceOnSearchTermSet(searchTerm);
  }

  /**
   * Reset input if Escape is pressed.
   */
  handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Escape') {
      this.setState({ searchTerm: '' });
      this.props.onSearchTermChange!('');
      e.stopPropagation();
    }
  }

  /**
   * Update the state of this Component, and call onSearchTermSet callback
   * immediately.
   */
  handleResetClick() {
    this.setState({ searchTerm: '' });
    this.props.onSearchTermChange!('');
  }

  render() {
    let { className, helpText, placeholderText } = this.props;
    let searchTerm = this.state.searchTerm;
    let isActiveSearch = searchTerm.length > 0;
    let showHelpIcon = (helpText != null && helpText != '');
    let activeModifier = isActiveSearch ? 'active' : 'inactive';
    let helpModifier = showHelpIcon ? 'withHelp' : '';
    return (
      <div className={classname(baseClassName, activeModifier, helpModifier)
        + ' ' + classname(className!, activeModifier, helpModifier)}>
        <label className={labelClassName}>
          <input type="search"
            ref={node => this.input = node}
            className={inputClassName}
            onChange={this.handleSearchTermChange}
            onKeyDown={this.handleKeyDown}
            placeholder={placeholderText}
            value={searchTerm}
          />
          <i className={"fa fa-search " + searchIconClassName}/>
          <button className={cancelBtnClassName}
            type="button" onClick={this.handleResetClick}>
            <i className={"fa fa-close " + cancelIconClassName}/>
          </button>
        </label>
        {showHelpIcon &&
          <Tooltip content={helpText!}>
            <i className={"fa fa-question-circle " + infoIconClassName}/>
          </Tooltip>
        }
      </div>
    );
  }
}

/**
 * Produce BEM style class names. The return value is a space-delimited
 * list of class names. The first is `base`, and the rest are generated
 * by joining `base` and each of `modifiers` with '__'.
 *
 * @example
 * let allClassNames = className('Thing', 'active', 'blue');
 * //=> 'Thing Thing__active Thing__blue'
 *
 * @param {string} base
 * @param {string} ...modifiers
 * @returns {string}
 */
function classname(base: string, ...modifiers: string[]) {
  return modifiers.reduce((classnames, modifier) => {
    return modifier ? classnames + ' ' + base + '__' + modifier : classnames;
  }, base);
}