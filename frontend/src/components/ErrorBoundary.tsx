import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('UI error boundary', error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="rounded-sm border border-frost bg-mist p-6" role="alert">
          <h2 className="font-serif text-2xl text-ink">This view could not be displayed</h2>
          <p className="mt-2 text-copy">The rest of the workspace is still available. Reload this page to try again.</p>
          <button type="button" className="btn-primary mt-4" onClick={() => this.setState({ hasError: false })}>
            Try again
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
