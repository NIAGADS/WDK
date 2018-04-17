import React from 'react';

import Events from 'Mesa/Utils/Events';
import Store from 'Mesa/State/Store';
import Icon from 'Mesa/Components/Icon';
import Templates from 'Mesa/Templates';
import { toggleSortOrder, sortByColumn, filterByColumnValues, toggleColumnFilterValue } from 'Mesa/State/Actions';

class HeadingRow extends React.Component {
  constructor (props) {
    super(props);
    this.state = {
      showColumnFilter: null
    };
    this.getRenderer = this.getRenderer.bind(this);
    this.renderHeadingCell = this.renderHeadingCell.bind(this);
    this.handleSortClick = this.handleSortClick.bind(this);
    this.handleFilterClick = this.handleFilterClick.bind(this);
    this.renderFilterMenu = this.renderFilterMenu.bind(this);
    this.openFilterMenu = this.openFilterMenu.bind(this);
    this.closeFilterMenu = this.closeFilterMenu.bind(this);
  }

  getRenderer (column) {
    if ('renderHeading' in column) return column.renderHeading(column);
    switch (column.type) {
      case 'text':
      default:
        return Templates.heading(column);
    }
  }

  handleSortClick ({ column }) {
    let { sort, filter } = Store.getState();
    let currentlySorting = sort.byColumn === column;
    if (currentlySorting) Store.dispatch(toggleSortOrder());
    else Store.dispatch(sortByColumn(column));
  }

  renderSortTrigger (column) {
    let { sort } = Store.getState();
    let isActiveSort = sort.byColumn === column;
    let sortIcon = !isActiveSort
      ? 'sort-amount-asc inactive'
      : sort.ascending
        ? 'sort-amount-asc'
        : 'sort-amount-desc';
    let sortTrigger = !column.sortable ? null : (
      <Icon
        fa={sortIcon + ' Trigger SortTrigger'}
        onClick={() => this.handleSortClick({ column })}
      />
    );
    return sortTrigger;
  }

  toggleColumnFilterValue (value) {
    Store.dispatch(toggleColumnFilterValue(value));
  }

  openFilterMenu (column) {
    this.setState({ showColumnFilter: column });
    this.filterCloseListener = Events.add('click', (e) => {
      let within = e.path.includes(this.refs.active);
      if (!within) this.closeFilterMenu();
    });
  }

  closeFilterMenu () {
    Events.remove(this.filterCloseListener);
    this.setState({ showColumnFilter: null });
  }

  handleFilterClick ({ column }) {
    let { showColumnFilter } = this.state;
    if (showColumnFilter !== column) this.openFilterMenu(column);
    else return this.closeFilterMenu();

    let { byColumn } = Store.getState().filter;
    if (byColumn === column) return;

    let { rows } = this.props;
    let values = Array.from(new Set(rows.map(row => row[column.key])));
    Store.dispatch(filterByColumnValues(column, values));
  }

  renderFilterTrigger (column) {
    let { filter } = Store.getState();
    let isFilterActive = filter.byColumn === column;
    let icon = isFilterActive
      ? 'filter'
      : 'filter inactive';
    let filterTrigger = !column.filterable ? null : (
      <Icon
        fa={icon + ' Trigger FilterTrigger'}
        onClick={() => this.handleFilterClick({ column })}
      />
    );
    return filterTrigger;
  }

  renderFilterMenu (column) {
    let { showColumnFilter } = this.state;
    let { rows } = this.props;
    let { valueWhitelist } = Store.getState().filter;
    let possibleValues = Array.from(new Set(rows.map(row => row[column.key])));
    let filterMenu = showColumnFilter !== column ? null : (
      <div className="FilterMenu">
        Filter by <b>{column.name || column.key}</b>:
        <div className="FilterMenu-CheckList">
          {possibleValues.map(value => {
            let valueIsChecked = valueWhitelist.includes(value);
            return (
              <div key={value} onClick={() => this.toggleColumnFilterValue(value)}>
                <Icon fa={valueIsChecked ? 'check-square' : 'square' } />
                {value}
              </div>
            );
          })}
        </div>
      </div>
    );
    return filterMenu;
  }

  renderHeadingCell (column) {
    let { showColumnFilter } = this.state;
    let content = this.getRenderer(column);
    let sortTrigger = this.renderSortTrigger(column);
    let filterTrigger = this.renderFilterTrigger(column);
    let filterMenu = this.renderFilterMenu(column);

    return column.hidden ? null : (
      <th key={column.key} ref={showColumnFilter === column ? 'active' : null}>
        {sortTrigger}
        {content}
        {filterTrigger}
        {filterMenu}
      </th>
    );
  }

  render () {
    const { columns } = this.props;
    const columnList = columns.map(this.renderHeadingCell);

    return (
      <tr className="HeadingRow">
        {columnList}
      </tr>
    );
  }
};

export default HeadingRow;
