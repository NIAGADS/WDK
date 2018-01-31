import { escapeRegExp, intersection, memoize, range } from 'lodash';
import React, { ValidationMap } from 'react';

import { ParamInitAction } from '../../actioncreators/QuestionActionCreators';
import CheckboxTree from '../../components/CheckboxTree';
import Icon from '../../components/IconAlt';
import { Action } from '../../dispatcher/Dispatcher';
import { makeActionCreator, payload } from '../../utils/ActionCreatorUtils';
import { filterNodes, getLeaves, isBranch } from '../../utils/TreeUtils';
import { ListEnumParam, TreeBoxVocabNode, TreeBoxEnumParam, Parameter } from '../../utils/WdkModel';
import { isType as isEnumParam } from '../EnumParam';
import { Context, Props } from '../Utils';
import SelectionInfo from './SelectionInfo';
import { Seq } from '../../utils/IterableUtils';
import { safeHtml } from '../../utils/componentUtils';


// Types
// -----

type Ctx = Context<TreeBoxEnumParam>;

type TreeBoxProps = Props<TreeBoxEnumParam, State>;

export type State = {
  expandedList: string[];
  searchTerm: string;
}


// ActionCreators
// --------------

const ExpandedListSet = makeActionCreator('enum-param-treebox/expanded-list-set',
  payload<Ctx & {
    expandedList: string[]
  }>());

const SearchTermSet = makeActionCreator('enum-param-treebox/search-term-set',
  payload<Ctx & {
    searchTerm: string
  }>());


// Utils
// -----

export function isType(parameter: Parameter): parameter is TreeBoxEnumParam {
  return (
    isEnumParam(parameter) &&
    parameter.displayType === 'treeBox'
  );
}

function searchPredicate(node: TreeBoxVocabNode, searchTerms: string[]) {
  return searchTerms
    .map(term => new RegExp(escapeRegExp(term), 'i'))
    .every(re => re.test(node.data.display));
}

function getNodeId(node: TreeBoxVocabNode) {
  return node.data.term;
}

function getNodeChildren(node: TreeBoxVocabNode) {
  return node.children;
}

function removeBranches(tree: TreeBoxVocabNode, items: string[]) {
  const leaves = getLeaves(tree, getNodeChildren);
  return intersection(leaves.map(getNodeId), items);
}

function deriveSelectedBranches(tree: TreeBoxVocabNode, items: string[]) {
  const itemSet = new Set(items);
  return filterNodes((node: TreeBoxVocabNode) => (
    isBranch(node, getNodeChildren) &&
    node.children.every(child => itemSet.has(child.data.term))
  ), tree)
    .map((node: TreeBoxVocabNode) => node.data.term);
}

function findBranchTermsUpToDepth(tree: TreeBoxVocabNode, depth: number): string[] {
  if (depth === 0) return [];
  return Seq.from(tree.children)
    .flatMap(node => [ node.data.term, ...findBranchTermsUpToDepth(node, depth - 1)])
    .toArray();
}

function deriveIndeterminateBranches(tree: TreeBoxVocabNode, items: string[]) {
  const itemSet = new Set(items);
  return filterNodes((node: TreeBoxVocabNode) => (
    isBranch(node, getNodeChildren) &&
    node.children.some(child => itemSet.has(child.data.term)) &&
    node.children.some(child => !itemSet.has(child.data.term))
  ), tree)
  .map((node: TreeBoxVocabNode) => node.data.term);
}


// Reducer
// -------

export function reduce(state: State, action: Action): State {
  if (
    ParamInitAction.isType(action) &&
    isType(action.payload.parameter)
  ) return {
    // FIXME Use depthExpanded instead
    expandedList: findBranchTermsUpToDepth(
      action.payload.parameter.vocabulary,
      action.payload.parameter.depthExpanded
    ),
    searchTerm: ''
  }

  if (ExpandedListSet.isType(action)) return {
    ...state,
    expandedList: action.payload.expandedList
  };

  if (SearchTermSet.isType(action)) return {
    ...state,
    searchTerm: action.payload.searchTerm
  }

  return state;
}


// View
// ----


export function TreeBoxEnumParam(props: TreeBoxProps) {
  const tree = props.parameter.vocabulary;
  const selectedNodes = props.value.split(/\s*,\s*/);
  const selectedLeaves = removeBranches(tree, selectedNodes);
  const selectedCount = props.parameter.countOnlyLeaves
  ? selectedLeaves.length
  : selectedNodes.length;
  return (
    <div className="wdk-TreeBoxParam">
      <SelectionInfo parameter={props.parameter} selectedCount={selectedCount}/>
      <CheckboxTree
        isSelectable={true}
        isMultiPick={props.parameter.multiPick}
        linksPosition={CheckboxTree.LinkPlacement.Bottom}
        showRoot={false}
        tree={tree}
        getNodeId={getNodeId}
        getNodeChildren={getNodeChildren}
        nodeComponent={VocabNodeRenderer}
        onExpansionChange={expandedList => props.dispatch(ExpandedListSet.create({ ...props.ctx, expandedList}))}
        expandedList={props.uiState.expandedList}
        selectedList={selectedLeaves}
        onSelectionChange={(ids: string[]) => {
          const idsWithBranches = ids.concat(deriveSelectedBranches(tree, ids));
          props.onParamValueChange(idsWithBranches.join(','));
        }}
        isSearchable={true}
        searchBoxPlaceholder="Filter list below..."
        searchIconName="filter"
        noResultsComponent={NoResults}
        searchTerm={props.uiState.searchTerm}
        searchPredicate={searchPredicate}
        onSearchTermChange={searchTerm => props.dispatch(SearchTermSet.create({ ...props.ctx, searchTerm }))}
      />
    </div>
  );
}

type VocabNodeRendererProps = {
  node: TreeBoxVocabNode;
}

function VocabNodeRenderer(props: VocabNodeRendererProps) {
  return safeHtml(props.node.data.display);
}

type NoResultsProps = {
  searchTerm: string;
}

function NoResults(props: NoResultsProps) {
  return (
    <div style={{ padding: '1em' }}>
      <Icon fa="warning"/> The string <strong>{props.searchTerm}</strong> did not match anything for this parameter.
    </div>
  )
}