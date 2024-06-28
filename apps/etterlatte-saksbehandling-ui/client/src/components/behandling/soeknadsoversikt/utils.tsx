import { StatusIconProps } from '~shared/icons/statusIcon'
import { JaNei } from '~shared/types/ISvar'
import { KildePdl } from '~shared/types/kilde'
import { formaterDato } from '~utils/formatering/dato'
import { GrunnlagKilde } from '~shared/types/grunnlag'

export const svarTilStatusIcon = (svar: JaNei | undefined): StatusIconProps => {
  switch (svar) {
    case JaNei.JA:
      return 'success'
    case JaNei.NEI:
      return 'error'
    default:
      return 'warning'
  }
}

export const formaterKildePdl = (kilde?: KildePdl) => {
  return kilde ? kilde.navn.toUpperCase() + ': ' + formaterDato(kilde.tidspunktForInnhenting) : undefined
}

export const formaterGrunnlagKilde = (kilde?: GrunnlagKilde) => {
  return kilde ? kilde.type.toUpperCase() + ': ' + formaterDato(kilde.tidspunkt) : undefined
}
