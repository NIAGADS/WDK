export default {

  Answer: mirrorProps(
    'LOADING',
    'LOAD_SUCCESS',
    'LOAD_ERROR'
  ),

  QuestionList: mirrorProps(
    'LOADING',
    'LOAD_SUCCESS',
    'LOAD_ERROR'
  ),

  Project: mirrorProps(
    'LOADING',
    'LOAD_SUCCESS',
    'LOAD_ERROR'
  ),

  User: mirrorProps(
    'LOADING',
    'LOAD_SUCCESS',
    'LOAD_ERROR'
  )

};

function mirrorProps(...props) {
  return props.reduce(function(acc, prop) {
    if (typeof prop !== 'string') {
      throw new TypeError(`Action type ${prop} is not a string`);
    }
    return acc[prop] = new String(prop), acc;
  }, {});
}
