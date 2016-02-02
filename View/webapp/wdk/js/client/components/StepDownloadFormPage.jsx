import React from 'react';
import { wrappable, filterOutProps } from '../utils/componentUtils';
import StepDownloadForm from './StepDownloadForm';

let NO_REPORTER_SELECTED = "_none_";

let ReporterOption = React.createClass({
  render() {
    let reporter = this.props.reporter;
    return ( <option value={reporter.name}>{reporter.displayName}</option> );
  }
});

let ReporterSelect = React.createClass({
  render() {
    let { reporters, selected, onChange } = this.props;
    if (reporters.length == 0) return null;
    return (
      <div>
        <span style={{marginRight:'0.5em'}}>Choose a Reporter:</span>
        <select value={selected} onChange={onChange}>
          <option value={NO_REPORTER_SELECTED}>Please Select...</option>
          {reporters.filter(f => f.isInReport).map(reporter =>
            ( <ReporterOption key={reporter.name} reporter={reporter}/> ))}
        </select>
      </div>
    );
  }
});

let ReporterSubmit = React.createClass({
  render() {
    let { reporters, selected, onSubmit } = this.props;
    if (reporters.length == 0 // show button since standard JSON form should appear
        || selected != NO_REPORTER_SELECTED) { // show since some form is selected
      return (
        <div style={{width:'30em',textAlign:'center', margin:'0.6em 0'}}>
          <input type="button" value="Submit" onClick={this.props.onSubmit}/>
        </div>
      );
    }
    else {
      return null;
    }
  }
});

let StepDownloadFormPage = React.createClass({

  changeReporter(event) {
    // convert "none" back to null value
    let newValue = event.target.value;
    if (newValue === NO_REPORTER_SELECTED) {
      newValue = null;
    }
    this.props.onReporterChange(newValue);
  },

  render() {

    // get the props needed in this component's render
    let { selectedReporter, recordClass, onFormSubmit } = this.props;

    // filter props we don't want to send to the child form
    let formProps = filterOutProps(this.props, [ 'onReporterChange', 'onFormSubmit' ]);

    // incoming store value of null indicates no format currently selected
    if (selectedReporter == null) {
      selectedReporter = NO_REPORTER_SELECTED;
    }

    // only use reporters configured for the report download page
    let reporters = recordClass.formats.filter(f => f.isInReport);

    return (
      <div style={{margin: '1em 3em'}}>
        <h1>Download Step Result: {this.props.step.displayName}</h1>
        <ReporterSelect reporters={reporters} selected={selectedReporter} onChange={this.changeReporter}/>
        <div>
          <StepDownloadForm {...formProps}/>
        </div>
        <ReporterSubmit reporters={reporters} selected={selectedReporter} onSubmit={onFormSubmit}/>
      </div>
    );
  }

});

export default wrappable(StepDownloadFormPage);