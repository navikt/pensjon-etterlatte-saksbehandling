import { isFailure, Result } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import React from 'react'

interface Wrappercomponent {
  component: React.ComponentType
  props: any
}
export const isFailureHandler = ({
  wrapperComponent = undefined,
  errorMessage,
  apiResult,
}: {
  wrapperComponent?: Wrappercomponent
  errorMessage: string
  apiResult: Result<any>
}): React.ReactElement | null => {
  if (isFailure(apiResult)) {
    if (apiResult.error.status === 502) {
      return null
    } else {
      const standardApiError = <ApiErrorAlert>{apiResult.error.detail || errorMessage}</ApiErrorAlert>
      if (wrapperComponent) {
        const Component = wrapperComponent.component
        return <Component {...wrapperComponent.props}>{standardApiError}</Component>
      } else {
        return standardApiError
      }
    }
  } else {
    return null
  }
}
