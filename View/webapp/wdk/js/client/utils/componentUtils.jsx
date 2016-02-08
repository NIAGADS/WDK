import React from 'react';

/**
 * React Component utils
 */

/**
 * A Component decorator that wraps the Component passed to it and returns a new
 * Component with a static method `wrap`. Use this method to replace the wrapped
 * Component. See the docs for `wrap` for more details.
 *
 *
 * Rationale
 * =========
 *
 * Components in WDK should be customizable by the implementor. The implementor
 * should be able to replace the Component, surround the Component, or modify
 * the props passed to the Component. By creating a wrapper Component whose
 * internal Component can be replaced in-place, WDK code does not need to do
 * any kind of lookup, and implementors are given a nice, convenient syntax for
 * what will most likely be a common task: modifying a WDK Component.
 *
 * There are other approaches that would require a uniform JavaScript build
 * environment, which would allow the implementor to perform overriding at
 * build time (similar to how we do this with JSP tags). We aren't quite at a
 * point where we can do this, and it might not be desirable.
 *
 *
 * Usage
 * =====
 *
 *     // As a function
 *     let Answer = wrappable(Answer);
 *
 *
 *     // As an ES2016 class decorator
 *     @wrappable
 *     class Answer extends React.Component {
 *       // ...
 *     }
 *
 *     // Using the static wrap method. Note that the function passed to `wrap`
 *     // will receive the currently wrapped Component as an argument. This
 *     // makes it possible to replace or decorate the Component, or to modify
 *     // the props passed to the component.
 *     Answer.wrap(function(Answer) {
 *       return React.createClass({
 *         render() {
 *           return (
 *             <div>
 *               <h1>My Custom title</h1>
 *               <Answer {...this.props}/>
 *             </div>
 *           );
 *         }
 *       });
 *     });
 *
 */
export function wrappable(Component) {
  return class Wrapper extends React.Component {

    // Forward calls for displayName and propTypes to the wrapped Component.
    // This is useful for debugging messages generated by React.

    static get displayName() {
      return `WdkWrapper(${Component.displayName || Component.name})`;
    }

    static get propTypes() {
      return Component.propTypes;
    }

    /**
     * Used to modify the Component being wrapped. Use it by passing a function
     * that returns a new Component.
     *
     * The function will receive the current Component as an argument. This
     * makes it possible to render the current Component in the new component,
     * similar to Aspect Oriented Programming techniques.
     *
     * The new Component will replace the existing Component.
     *
     * TODO Verify that factory returns a React class
     *
     * @param {function} factory A factory function returning a new React
     *  Component. The function will receive the current Component as its sole
     *  param.
     */
    static wrapComponent(factory) {
      Component = factory(Component);
    }

    render() {
      return (
        <Component {...this.props}/>
      );
    }

  };
}

export function safeHtml(str, props = null, Component = 'span') {
  return <Component {...props} dangerouslySetInnerHTML={{__html: str}}/>
}

/**
 * Makes a copy of the passed original object, subtracting the properties with
 * names in the propsToFilter arg, which should be Array[String].
 */
export function filterOutProps(orig, propsToFilter) {
  return Object.keys(orig).reduce((obj, key) =>
    (propsToFilter.indexOf(key) !== -1 ? obj :
      Object.assign(obj, { [key]: orig[key] })), {});
}

/**
 * Generates HTML markup for an attribute using duck-typing
 */
export function formatAttributeValue(value) {
  return (Object(value) === value && 'url' in value)
    ? `<a href="${value.url}">${value.displayText || value.url}</a>`
    : value;
}

/**
 * Creates a React-renderable element using the provided `Component`, or 'span'
 * by default.
 * TODO Look up or inject custom formatters
 */
export function renderAttributeValue(value, props = null, Component = 'span') {
  return safeHtml(
    formatAttributeValue(value),
    props,
    Component
  );
}

/**
 * Makes a copy of current, adds value if not present, removes if present, and
 * returns the copy.
 * @param {Array<T>} array array to modify
 * @param {<T>} value to check against
 * @return {Array<T>} modified copy of original array
 */
export function addOrRemove(array, value) {
  return (array.indexOf(value) == -1 ?
    // not currently present; add
    array.concat(value) :
    // already there; remove
    array.filter(elem => elem != value));
}

/**
 * Looks for the property with the passed name in the given object.  If the
 * object or the property is null or undefined, returns default value.
 * Otherwise returns the value found.
 */
export function getValueOrDefault(object, propertyName, defaultValue) {
  return (object == null || object == undefined ||
      object[propertyName] == null || object[propertyName] == undefined ?
      defaultValue : object[propertyName]);
}
