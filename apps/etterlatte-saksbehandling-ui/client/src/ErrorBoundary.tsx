import React, { ErrorInfo, ReactNode } from 'react'
import { logger } from '~utils/logger'
import ErrorStackParser from 'error-stack-parser'
import styled from 'styled-components'
import { Alert } from '@navikt/ds-react'
import { Link } from 'react-router-dom'

type Props = {
  children: React.JSX.Element
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
          message: error.message,
          error: JSON.stringify(error),
        })
      } catch {
        logger.generalError({ msg: 'componentDidCatch, kan ikke parse stackframes', err: error, errorInfo })
      }
    } else {
      logger.generalError({ msg: 'componentDidCatch, ingen stackframes tilgjengelig', err: error, errorInfo })
    }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div>
          <ApiErrorAlert>En feil har oppstått og blitt logget.</ApiErrorAlert>
          <HjemLink to="/" onClick={() => this.setState({ hasError: false })}>
            Gå til hovedskjermen
          </HjemLink>
        </div>
      )
    }
    return this.props.children
  }
}

export default ErrorBoundary

export const ApiErrorAlert = ({ children }: { children: ReactNode | Array<ReactNode> }) => {
  return <BredAlert variant="error">{children}</BredAlert>
}

export const ApiWarningAlert = ({ children }: { children: ReactNode | Array<ReactNode> }) => {
  return <BredAlert variant="warning">{children}</BredAlert>
}

const BredAlert = styled(Alert)`
  margin: 2rem auto;
  max-width: fit-content;
`

const HjemLink = styled(Link)`
  margin: 2rem auto;
  max-width: fit-content;
  display: block;
`
