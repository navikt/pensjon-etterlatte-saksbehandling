import React, { ErrorInfo } from 'react'
import { logErrorWithStacktraceJS } from '~utils/logger'

type Props = {
  children: JSX.Element
}

class ErrorBoundary extends React.Component<Props, { hasError: boolean }> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('componentDidCatch errorinfo ', errorInfo, '\nerror', error)
    logErrorWithStacktraceJS(error)
  }

  render() {
    if (this.state.hasError) {
      return <div>En feil har oppstått og blitt logget.</div>
    }
    return this.props.children
  }
}

export default ErrorBoundary
