import React from 'react';
import {includes, memoize, throttle} from 'lodash';
import { seq } from '../utils/IterableUtils';
import { preorderSeq, pruneDescendantNodes } from '../utils/TreeUtils';
import { wrappable, PureComponent } from '../utils/componentUtils';
import { getId, isIndividual } from '../utils/CategoryUtils';
import RecordNavigationItem from './RecordNavigationItem';
import CategoriesCheckboxTree from './CategoriesCheckboxTree';

/** Navigation panel for record page */
class RecordNavigationSection extends PureComponent {

  constructor(props) {
    super(props);
    this.handleSearchTermChange = this.handleSearchTermChange.bind(this);
    this.setActiveCategory = throttle(this.setActiveCategory.bind(this), 300);
    this.state = { activeCategory: null };
  }

  componentDidMount() {
    window.addEventListener('scroll', this.setActiveCategory, { passive: true });
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this.setActiveCategory, { passive: true });
  }

  componentDidUpdate(previousProps) {
    if (this.props.collapsedSections !== previousProps.collapsedSections ||
        this.props.showChildren !== previousProps.showChildren ) {
      this.setActiveCategory();
    }
  }

  setActiveCategory() {
    let { categoryTree, navigationCategoriesExpanded } = this.props;
    let activeCategory = seq(removeFields(categoryTree).children)
      // transform each top-level node into a list of all nodes of that branch
      // of the tree that are visible in this section
      .flatMap(topLevelNode => [
        topLevelNode,
        ...preorderSeq(topLevelNode)
          .filter(node => navigationCategoriesExpanded.includes(getId(node)))
          .flatMap(node => node.children)
      ])
      // find the category whose content is near the top of the viewport
      .findLast(node => {
        let id = getId(node);
        let domNode = document.getElementById(id);
        if (domNode == null) return;
        let rect = domNode.getBoundingClientRect();
        return rect.top <= 70;
      });

    this.setState({ activeCategory });
  }

  handleSearchTermChange(term) {
    this.props.onNavigationQueryChange(term);
  }

  render() {
    let {
      categoryTree,
      collapsedSections,
      heading,
      navigationQuery,
      navigationCategoriesExpanded,
      onNavigationCategoryExpansionChange,
      onSectionToggle
    } = this.props;

    return (
      <div className="wdk-RecordNavigationSection">
        <h2 className="wdk-RecordNavigationSectionHeader">
          {heading}
        </h2>
        <CategoriesCheckboxTree
          searchBoxPlaceholder="Search section names..."
          tree={removeFields(categoryTree)}
          leafType="section"
          isSelectable={false}
          expandedBranches={navigationCategoriesExpanded}
          onUiChange={onNavigationCategoryExpansionChange}
          searchTerm={navigationQuery}
          onSearchTermChange={this.handleSearchTermChange}
          nodeComponent={props =>
            <RecordNavigationItem
              {...props}
              onSectionToggle={onSectionToggle}
              activeCategory={this.state.activeCategory}
              checked={!includes(collapsedSections, getId(props.node))}
            />
          }
        />
      </div>
    );
  }
}

RecordNavigationSection.propTypes = {
  collapsedSections: React.PropTypes.array,
  onSectionToggle: React.PropTypes.func,
  heading: React.PropTypes.node
};

RecordNavigationSection.defaultProps = {
  onSectionToggle: function noop() {},
  heading: 'Contents'
};

export default wrappable(RecordNavigationSection);

const removeFields = memoize(root =>
  pruneDescendantNodes(node => !isIndividual(node), root));
