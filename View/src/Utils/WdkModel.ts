/**
 * Type definitions for WDK Model entities
 */

import { Field } from "Components/AttributeFilter/Utils/FilterService";

interface ModelEntity {
  name: string;
  displayName: string;
  properties?: Record<string, string>;
}

export interface RecordClass extends ModelEntity {
  displayNamePlural: string;
  recordIdAttributeName: string;
  primaryKeyColumnRefs: string[];
  description: string;
  urlSegment: string;
  attributes: AttributeField[];
  tables: TableField[];
  attributesMap: Record<string, AttributeField>;
  tablesMap: Record<string, TableField>;
  formats: Reporter[];
  useBasket: boolean;
}

export interface Reporter {
  name: string;
  displayName: string;
  description: string;
  isInReport: boolean;
  scopes: string[];
}

export interface ParameterBase extends ModelEntity {
  help: string;
  isVisible: boolean;
  group: string;
  isReadOnly: boolean;
  defaultValue: ParameterValue;
  dependentParams: string[];
}

export interface StringParam extends ParameterBase {
  type: 'StringParam';
}

export interface FilterParamNew extends ParameterBase {
  type: 'FilterParamNew';
  filterDataTypeDisplayName?: string;
  ontology: Array<{
    term: string;
    parent: string;
    display: string;
    description: string;
    type: any;
    units: string;
    precision: string;
    isRange: boolean;
  }>;
  values: Record<string, string[]>;
}

export interface EnumParam extends ParameterBase {
  type: 'EnumParam' | 'FlatVocabParam';
  displayType: string;
  countOnlyLeaves: boolean;
  maxSelectedCount: number;
  minSelectedCount: number;
  multiPick: boolean;
  depthExpanded: number;
}

type VocabTerm = string;
type VocabDisplay = string;
type VocabParent = string;

export interface ListEnumParam extends EnumParam {
  displayType: 'select' | 'checkBox' | 'typeAhead';
  vocabulary: [ VocabTerm, VocabDisplay, VocabParent | null ][];
}

export interface TreeBoxVocabNode {
  data: {
    term: string;
    display: string;
  };
  children: TreeBoxVocabNode[]
}

export interface TreeBoxEnumParam extends EnumParam {
  displayType: 'treeBox';
  vocabulary: TreeBoxVocabNode;
}

export interface NumberParam extends ParameterBase {
  type: 'NumberParam';
  min: number;
  max: number;
  step: number;
}

export interface NumberRangeParam extends ParameterBase {
  type: 'NumberRangeParam';
  min: number;
  max: number;
  step: number;
}

export interface DateParam extends ParameterBase {
  type: 'DateParam';
  minDate: string;
  maxDate: string;
}

export interface DateRangeParam extends ParameterBase {
  type: 'DateRangeParam';
  minDate: string;
  maxDate: string;
}

export type Parameter = StringParam | EnumParam | FilterParamNew | NumberParam | NumberRangeParam | DateParam | DateRangeParam;

export interface ParameterGroup {
  description: string;
  displayName: string;
  displayType: string; // this should be a union of string literals
  isVisible: boolean;
  name: string;
  parameters: string[];
}

export interface Question extends ModelEntity {
  summary: string;
  description: string;
  shortDisplayName: string;
  recordClassName: string;
  help: string;
  newBuild: string;
  reviseBuild: string;
  urlSegment: string;
  class: string;
  parameters: Parameter[];
  groups: ParameterGroup[];
  defaultAttributes: string[];
  dynamicAttributes: AttributeField[];
  defaultSummaryView: string;
  summaryViewPlugins: string[];
  stepAnalysisPlugins: string[];
}

export type ParameterValue = string;

export type ParameterValues = Record<string, ParameterValue>;

export type OntologyTermSummary = {
  valueCounts: Array<{
    value: string | number,
    filteredCount: number,
    count: number
  }>;
  internalsCount: number;
  internalsFilteredCount: number;
};

export type SortSpec = {
  groupBySelected: boolean;
  columnKey: string;
  direction: string;
};

export type FieldState = {
  sort: SortSpec;
}

export type ParamUIState = { } | {
  errorMessage?: string;
  loading: boolean;
  activeOntologyTerm?: string;
  hideFilterPanel: boolean;
  hideFieldPanel: boolean;
  ontologyTermSummaries: Record<string, OntologyTermSummary>;
  defaultMemberFieldState: FieldState;
  fieldStates: Record<string, FieldState>;
  ontology: Field[];
  filteredCount?: number;
  unfilteredCount?: number;
}

export interface AttributeField extends ModelEntity {
  help: string;
  align: string;
  isSortable: boolean;
  isRemovable: boolean;
  type: string;
  truncateTo: number;
}

export interface TableField extends ModelEntity {
  help: string;
  type: string;
  description: string;
  attributes: AttributeField[];
}

export interface RecordInstance {
  displayName: string;
  id: PrimaryKey;
  recordClassName: string;
  attributes: Record<string, AttributeValue>;
  tables: Record<string, TableValue>;
  tableErrors: string[];
}

export interface PrimaryKey extends Array<{
  name: string;
  value: string;
}> {}

export type AttributeValue = string | LinkAttributeValue;

export interface LinkAttributeValue {
  url: string;
  displayText: string;
}

export interface TableValue extends Array<Record<string, AttributeValue>> { }

export interface Answer {
  records: RecordInstance[];
  meta: {
    attributes: string[];
    tables: string[];
    recordClassName: string;
    responseCount: number;
    totalCount: number;
  }
}

export interface AnswerSpec {
  questionName: string;
  parameters?: Record<string, string>;
  legacyFilterName?: string;
  filters?: { name: string; value: string; }[];
  viewFilters?: { name: string; value: string; }[];
  wdk_weight?: number;
}

export interface AnswerFormatting {
  format: string
  formatConfig: {}
}

export interface NewStepSpec {
  answerSpec: AnswerSpec,
  custonName?: string,
  isCollapsible?: boolean,
  collapsedName?: string
}

export type UserDatasetMeta = {
  description: string;
  name: string;
  summary: string;
};

export type UserDatasetShare = {
  time: number;
  user: number;
  userDisplayName: string;
};

export type UserDataset = {
  created: number;
  isInstalled: boolean;
  dependencies: Array<{
    resourceDisplayName: string;
    resourceIdentifier: string;
    resourceVersion: string;
  }>;
  datafiles: Array<{
    name: string;
    size: number;
  }>;
  projects: string[];
  id: number;
  meta: UserDatasetMeta;
  modified: number;
  owner: string;
  ownerUserId: number;
  percentQuotaUsed: number;
  sharedWith: UserDatasetShare[] | undefined;
  questions: string[];
  size: number;
  type: {
    name: string;
    version: string;
  };
  updloaded: number;
}

export type Favorite = {
  id: number;
  recordClassName: string;
  primaryKey: PrimaryKey;
  displayName: string;
  description: string;
  group: string;
}

export function getSingleRecordQuestionName(recordClassName: string): string {
  return `__${recordClassName}__singleRecordQuestion__`;
}

export function getSingleRecordAnswerSpec(record: RecordInstance): AnswerSpec {
  return {
    questionName: getSingleRecordQuestionName(record.recordClassName),
    parameters: {
      "primaryKeys": record.id.map(pkCol => pkCol.value).join(",")
    }
  };
}