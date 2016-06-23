import {Component} from 'react';
import classnames from 'classnames';
import {wrappable} from '../utils/componentUtils';
import Main from './Main';
import Record from './Record';
import RecordHeading from './RecordHeading';
import RecordNavigationSection from './RecordNavigationSection';
import Sticky from './Sticky';

/**
 * Renders the main UI for the WDK Record page.
 */
class RecordUI extends Component {

  componentDidMount() {
    let { hash } = window.location;
    let target = document.getElementById(hash.slice(1));
    if (target != null) {
      target.scrollIntoView();
    }
  }

  render() {
    let classNames = classnames(
      'wdk-RecordContainer',
      'wdk-RecordContainer__' + this.props.recordClass.name,
      {
        'wdk-RecordContainer__withSidebar': this.props.navigationVisible      }
    );

    let sidebarIconClass = classnames({
      'fa fa-lg': true,
      'fa-angle-double-down': !this.props.navigationVisible,
      'fa-angle-double-up': this.props.navigationVisible
    });

    return (
      <Main className={classNames}>
        <RecordHeading
          record={this.props.record}
          recordClass={this.props.recordClass}
          headerActions={this.props.headerActions}
        />
        <Sticky className="wdk-RecordSidebar" fixedClassName="wdk-RecordSidebar__fixed">
          <button type="button" className="wdk-RecordSidebarToggle"
            onClick={() => {
              if (!this.props.navigationVisible) window.scrollTo(0, window.scrollY);
              this.props.updateNavigationVisibility(!this.props.navigationVisible);
            }}
          >
            {this.props.navigationVisible ? '' : 'Show Contents '}
            <i className={sidebarIconClass}
              title={this.props.navigationVisible ? 'Close sidebar' : 'Open sidebar'}/>
          </button>
          <RecordNavigationSection
            record={this.props.record}
            recordClass={this.props.recordClass}
            categoryTree={this.props.categoryTree}
            collapsedSections={this.props.collapsedSections}
            navigationQuery={this.props.navigationQuery}
            navigationExpanded={this.props.navigationExpanded}
            navigationSubcategoriesExpanded={this.props.navigationSubcategoriesExpanded}
            onSectionToggle={this.props.toggleSection}
            onNavigationVisibilityChange={this.props.updateNavigationVisibility}
            onNavigationSubcategoryVisibilityChange={this.props.updateNavigationSubcategoryVisibility}
            onNavigationQueryChange={this.props.updateNavigationQuery}
          />
        </Sticky>
        <div className="wdk-RecordMain">
          <div className="wdk-RecordMainSectionFieldToggles">
            <button type="button" title="Expand all content" className="wdk-Link"
              onClick={this.props.updateAllFieldVisibility.bind(null, true)}>Expand All</button>
            {' | '}
            <button type="button" title="Collapse all content" className="wdk-Link"
              onClick={this.props.updateAllFieldVisibility.bind(null, false)}>Collapse All</button>
          </div>
          <Record
            record={this.props.record}
            recordClass={this.props.recordClass}
            categoryTree={this.props.categoryTree}
            collapsedSections={this.props.collapsedSections}
            onSectionToggle={this.props.toggleSection}
          />
        </div>
      </Main>
    )
  }
}

export default wrappable(RecordUI);
