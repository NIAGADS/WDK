import React from 'react';
import Sidebar from './Sidebar';
import Main from './Main';
import RecordMainSection from './RecordMainSection';
import RecordHeading from './RecordHeading';
import RecordNavigationSection from './RecordNavigationSection';
import { wrappable } from '../utils/componentUtils';
import {
  formatAttributeValue
} from '../utils/stringUtils';


let Record = React.createClass({

  propTypes: {
    record: React.PropTypes.object.isRequired,
    recordClass: React.PropTypes.object.isRequired,
    questions: React.PropTypes.array.isRequired,
    recordClasses: React.PropTypes.array.isRequired,
    recordActions: React.PropTypes.object.isRequired,
    hiddenCategories: React.PropTypes.array.isRequired,
    collapsedCategories: React.PropTypes.array.isRequired
  },

  handleCollapsedChange({ category, isCollapsed }) {
    let { recordClass } = this.props;
    this.props.recordActions.toggleCategoryCollapsed({
      recordClass,
      category,
      isCollapsed
    });
  },

  render() {
    let { recordClass } = this.props;
    return (
      <div className="wdk-Record">
        <Sidebar>
          <RecordNavigationSection
            {...this.props}
            categories={recordClass.attributeCategories}
            onCollapsedChange={this.handleCollapsedChange}
          />
          <p style={{ padding: '0 .6em' }}><a href="#top">Back to top</a></p>
        </Sidebar>
        <Main withSidebar="true">
          <RecordHeading {...this.props}/>
          <RecordMainSection
            {...this.props}
            categories={recordClass.attributeCategories}
          />
        </Main>
      </div>
    );
  }
});

export default wrappable(Record);
