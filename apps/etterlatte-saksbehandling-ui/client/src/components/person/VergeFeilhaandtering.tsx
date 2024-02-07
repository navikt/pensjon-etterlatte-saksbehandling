import { ApiErrorAlert } from '~ErrorBoundary'
import { isFailure, Result } from '~shared/api/apiUtils'
import { Mottaker } from '~shared/types/Brev'

export const VergeFeilhaandtering = (result: Result<Mottaker>) => {
  if (isFailure(result) && result.error.status != 404) {
    return <ApiErrorAlert>Feil oppsto ved henting av eventuell verges adresse. Prøv igjen senere</ApiErrorAlert>
  }
}
