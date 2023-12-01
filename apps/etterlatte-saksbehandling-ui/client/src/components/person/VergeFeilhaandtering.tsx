import { ApiErrorAlert } from '~ErrorBoundary'

import { isFailure, Result } from '~shared/api/apiUtils'

export const VergeFeilhaandtering = (result: Result<any>) => {
  if (isFailure(result) && result.error.status != 404) {
    return <ApiErrorAlert>Feil oppsto ved henting av eventuell verges adresse. Prøv igjen senere</ApiErrorAlert>
  }
}
