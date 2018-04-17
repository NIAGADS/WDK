import React from 'react';

function getRealOffset (el) {
  let top = 0;
  let left = 0;
  do {
    top += el.offsetTop || 0;
    left += el.offsetLeft || 0;
    el = el.offsetParent;
  } while (el);
  return { top, left };
};

class Tooltip extends React.Component {
  constructor (props) {
    super(props);
    this.state = { showText: false, position: { top: 0, left: 0 } }
    this.showTooltip = this.showTooltip.bind(this);
    this.hideTooltip = this.hideTooltip.bind(this);
    this.renderTextBox = this.renderTextBox.bind(this);
    this.componentDidMount = this.componentDidMount.bind(this);
  }

  componentDidMount () {
    const { anchor } = this.refs;
    if (!anchor) return;
    const position = getRealOffset(anchor);
    console.log('got position as', position);
    this.setState({ position });
  }

  showTooltip () {
    const showText = true;
    this.setState({ showText })
  }

  hideTooltip () {
    const showText = false;
    this.setState({ showText });
  }

  renderTextBox () {
    const { text } = this.props;
    const { showText, position } = this.state;

    const wrapperStyle = {
      position: 'fixed',
      top: 0,
      left: 0,
      width: '100vw',
      height: '100%',
      pointerEvents: 'none'
    };

    const textStyle = {
      position: 'relative',
      top: position.top + 'px',
      left: position.left + 'px'
    };

    return !showText ? null : (
      <div className="Tooltip-Wrapper" style={wrapperStyle}>
        <div className="Tooltip-Text" style={textStyle}>
          {text}
        </div>
      </div>
    );
  }

  render () {
    const { children } = this.props;
    const className = 'Tooltip' + (this.props.className ? ' ' + this.props.className : '');
    const TextBox = this.renderTextBox;
    return (
      <div
        ref={'anchor'}
        className={className}
        onMouseEnter={this.showTooltip}
        onMouseLeave={this.hideTooltip}>
        {children}
        <TextBox />
      </div>
    )
  }
};

export default Tooltip;
