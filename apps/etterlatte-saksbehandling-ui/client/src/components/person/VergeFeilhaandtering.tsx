import { ApiErrorAlert } from '~shared/error/ApiErrorAlert'
import { isFailure, Result } from '~shared/api/apiUtils'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { Mottaker } from '~shared/types/Brev'
import { KildePersondata } from '~shared/types/kilde'

export const VergeFeilhaandtering = (result: Result<Grunnlagsopplysning<Mottaker, KildePersondata>>) => {
  if (isFailure(result) && result.error.status != 404) {
    return <ApiErrorAlert>Feil oppsto ved henting av eventuell verges adresse. Prøv igjen senere</ApiErrorAlert>
  }
}
