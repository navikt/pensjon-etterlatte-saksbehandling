import { ApiErrorAlert } from '~ErrorBoundary'
import { isFailure, Result } from '~shared/hooks/useApiCall'

export const handleHentVergeadresseError = (result: Result<any>) => {
  if (isFailure(result)) {
    if (result.error.status != 404) {
      console.error(`Feil ved opprettelse av revurdering, status: ${result.status} error: ${result.error.detail}`)
      return <ApiErrorAlert>Feil oppsto ved henting av eventuell verges adresse. Prøv igjen senere</ApiErrorAlert>
    }
  }
}
