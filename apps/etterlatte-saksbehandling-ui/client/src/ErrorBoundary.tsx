import React, { ErrorInfo } from 'react'
import { logger } from '~utils/logger'
import ErrorStackParser from 'error-stack-parser'
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
    const stackFrames = ErrorStackParser.parse(error)
    if (stackFrames.length > 0) {
      const stackFrame = stackFrames[0]
      try {
        logger.error({
          lineno: stackFrame.lineNumber!!,
          columnno: stackFrame.columnNumber!!,
          error: JSON.stringify(error),
        })
      } catch (e) {
        const errorobj = { err: error, errorInfo: errorInfo }
        logger.generalError(JSON.stringify(errorobj))
      }
    } else {
      const errorobj = { err: error, errorInfo: errorInfo }
      logger.generalError(JSON.stringify(errorobj))
    }
  }

  render() {
    if (this.state.hasError) {
      return <div>En feil har oppstått og blitt logget.</div>
    }
    return this.props.children
  }
}

export default ErrorBoundary
