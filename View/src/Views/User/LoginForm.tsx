import React, { Component, FormEvent } from 'react';
import { wrappable, getChangeHandler } from 'Utils/ComponentUtils';
import Dialog from 'Components/Overlays/Dialog';
import TextBox from 'Components/InputControls/TextBox';

type Props = {
  onCancel: () => void;
  onSubmit: (email: string, password: string) => void;
  open: boolean;
  message?: string;
  passwordResetUrl: string;
  registerUrl: string;
};

type State = {
  email: string,
  password: string
}

/**
 * Form used for authorizing against webapp instead of oauth server.
 */
class LoginForm extends Component<Props,State> {

  constructor(props: Props) {
    super(props);
    this.state = { email: '', password: '' };
    this.submitForm = this.submitForm.bind(this);
    this.updateState = this.updateState.bind(this);
  }

  submitForm(e: FormEvent<HTMLFormElement>): void {
    e.preventDefault();
    this.props.onSubmit(this.state.email, this.state.password);
  }

  updateState(newState: State): State {
    this.setState(newState);
    return newState;
  }

  render() {
    let { open, message, onCancel, passwordResetUrl, registerUrl } = this.props;
    let onChange = (name: string) => getChangeHandler(name, this.updateState, this.state);
    return open === false ? <noscript/> : (
      <Dialog title="Login" open={true} modal={true} onClose={onCancel}>
        <form onSubmit={this.submitForm} name="loginForm">
          {message ? <span style={{color:'red'}}>{message}</span> : ""}
          <table>
            <tbody>
              <tr>
                <td style={{ textAlign: 'right' }}>
                  <div className="small">
                    <b>Email: </b>
                  </div>
                </td>
                <td style={{ textAlign: 'left' }}>
                  <div className="small">
                    <TextBox size={20} value={this.state.email} onChange={onChange('email')}/>
                  </div>
                </td>
              </tr>
              <tr>
                <td style={{ textAlign: 'right' }}>
                  <div className="small">
                    <b>Password: </b>
                  </div>
                </td>
                <td style={{ textAlign: 'left' }}>
                  <div className="small">
                    <TextBox size={20} type="password" value={this.state.password} onChange={onChange('password')}/>
                  </div>
                </td>
              </tr>
              <tr>
                <td style={{ textAlign: 'center', whiteSpace: 'nowrap' }} colSpan={2}>
                  <span className="small">
                    <input style={{width:76, height:30, fontSize: '1em'}} id="login" value="Login" type="submit"/>
                    <input onClick={onCancel} style={{width:76, height:30, fontSize: '1em'}} value="Cancel" type="button"/>
                  </span>
                </td>
              </tr>
              <tr>
                <td style={{ textAlign: 'center', verticalAlign: 'top' }} colSpan={2}>
                  <span className="small">
                    <a href={passwordResetUrl} style={{paddingRight:15 }}>Forgot Password?</a>
                    <a href={registerUrl}>Register/Subscribe</a>
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </form>
      </Dialog>
    );
  }
}

export default wrappable(LoginForm);