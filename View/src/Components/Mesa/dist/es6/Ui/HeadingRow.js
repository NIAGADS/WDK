'use strict';var _createClass=function(){function defineProperties(target,props){for(var descriptor,i=0;i<props.length;i++)descriptor=props[i],descriptor.enumerable=descriptor.enumerable||!1,descriptor.configurable=!0,'value'in descriptor&&(descriptor.writable=!0),Object.defineProperty(target,descriptor.key,descriptor)}return function(Constructor,protoProps,staticProps){return protoProps&&defineProperties(Constructor.prototype,protoProps),staticProps&&defineProperties(Constructor,staticProps),Constructor}}(),_react=require('react'),_react2=_interopRequireDefault(_react),_HeadingCell=require('../Ui/HeadingCell'),_HeadingCell2=_interopRequireDefault(_HeadingCell),_SelectionCell=require('../Ui/SelectionCell'),_SelectionCell2=_interopRequireDefault(_SelectionCell),_Defaults=require('../Defaults');Object.defineProperty(exports,'__esModule',{value:!0});function _interopRequireDefault(obj){return obj&&obj.__esModule?obj:{default:obj}}function _classCallCheck(instance,Constructor){if(!(instance instanceof Constructor))throw new TypeError('Cannot call a class as a function')}function _possibleConstructorReturn(self,call){if(!self)throw new ReferenceError('this hasn\'t been initialised - super() hasn\'t been called');return call&&('object'==typeof call||'function'==typeof call)?call:self}function _inherits(subClass,superClass){if('function'!=typeof superClass&&null!==superClass)throw new TypeError('Super expression must either be null or a function, not '+typeof superClass);subClass.prototype=Object.create(superClass&&superClass.prototype,{constructor:{value:subClass,enumerable:!1,writable:!0,configurable:!0}}),superClass&&(Object.setPrototypeOf?Object.setPrototypeOf(subClass,superClass):subClass.__proto__=superClass)}var HeadingRow=function(_React$PureComponent){function HeadingRow(props){return _classCallCheck(this,HeadingRow),_possibleConstructorReturn(this,(HeadingRow.__proto__||Object.getPrototypeOf(HeadingRow)).call(this,props))}return _inherits(HeadingRow,_React$PureComponent),_createClass(HeadingRow,[{key:'render',value:function render(){var _props=this.props,options=_props.options,columns=_props.columns,actions=_props.actions,uiState=_props.uiState,eventHandlers=_props.eventHandlers,_ref=options?options:{},isRowSelected=_ref.isRowSelected,_ref2=uiState?uiState:{},sort=_ref2.sort,_ref3=eventHandlers?eventHandlers:{},onRowSelect=_ref3.onRowSelect,onRowDeselect=_ref3.onRowDeselect;return _react2.default.createElement('tr',{className:'Row HeadingRow'},'function'==typeof isRowSelected&&'function'==typeof onRowSelect&&'function'==typeof onRowDeselect?_react2.default.createElement(_SelectionCell2.default,{heading:!0,rows:rows,eventHandlers:eventHandlers,isRowSelected:isRowSelected}):null,columns.map(function(column){return _react2.default.createElement(_HeadingCell2.default,{key:column.key,column:column,sort:sort,eventHandlers:eventHandlers})}))}}]),HeadingRow}(_react2.default.PureComponent);exports.default=HeadingRow;