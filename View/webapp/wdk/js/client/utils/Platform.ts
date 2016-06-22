/**
 * Standard alerts. These can be thought of as platform-level utilities
 * to be used with action creators, etc, and should not be used in UI
 * components. The fact that they use the DOM is an implementation detail.
 */

import $ from 'jquery';

/**
 * @return {Promise<void>}
 */
export function alert(title: string, message: string) {
  return dialog(title, message, [
    { text: 'OK', focus: true }
  ]);
}

/**
 * @return {Promise<boolean>}
 */
export function confirm(title: string, message: string) {
  return dialog(title, message, [
    { text: 'Cancel', value: false },
    { text: 'OK', value: true, focus: true }
  ], false);
}

interface ButtonDescriptor {
  text: string;
  value?: any;
  focus?: boolean;
}

/**
 * @param {string} title
 * @param {string} message
 * @param {Array<ButtonDescriptor>} buttons
 * @param {any} escapeValue The value to use when dialog is closed via pressing the escape key
 * @returns {Promise<any>}
 */
export function dialog(title: string, message: string, buttons: ButtonDescriptor[], escapeValue?: any) {
  return new Promise(function(resolve, reject) {
    let $node = $('<div><p>' + message + '</p><div class="wdk-AlertButtons"></div></div>');
    let $buttons = buttons.map(button => {
      return $('<button>' + button.text + '</button>')
      .attr('autofocus', button.focus && 'autofocus')
      .click(() => {
        $node.dialog('close');
        resolve(button.value);
      });
    });
    $node.find('.wdk-AlertButtons').append($buttons);
    try {
      $node.dialog({
        title: title,
        modal: true,
        position: [ 'center', window.innerHeight * .3 ],
        resizable: false,
        dialogClass: 'wdk-Alert',
        minWidth: 350,
        open() {
          $node.parent().find('[autofocus]').focus();
        },
        close(event: KeyboardEvent) {
          if (event.key === 'Escape') {
            resolve(escapeValue);
          }
          $node.dialog('destroy').remove();
        }
      });
    }
    catch(err) {
      reject(err);
    }
  });
}