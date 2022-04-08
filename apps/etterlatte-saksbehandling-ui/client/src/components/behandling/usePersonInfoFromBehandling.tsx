import { useContext } from 'react'
import { AppContext } from '../../store/AppContext'
import { OpplysningsType } from '../../store/reducers/BehandlingReducer'
import { hentVirkningstidspunkt } from './soeknadsoversikt/utils'
import {
  IAvdoedFraSoeknad,
  IBarnFraSoeknad,
  IGjenlevendeFraSoeknad,
  IPersonOpplysning,
  IPersonOpplysningFraPdl,
} from './types'

export const usePersonInfoFromBehandling = () => {
  const ctx = useContext(AppContext)

  const grunnlag = ctx.state.behandlingReducer.grunnlag

  console.log('usePersonInfoHook', grunnlag)
  /*
    Todo: Dra ut i behandlingsHook
  */
  const soekerPdl: IPersonOpplysningFraPdl = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_pdl
  )?.opplysning

  const soekerSoknad: IBarnFraSoeknad = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_soeknad
  )?.opplysning

  const avdoedPersonPdl: IPersonOpplysningFraPdl = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_forelder_pdl
  )?.opplysning

  const avdodPersonSoknad: IAvdoedFraSoeknad = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_forelder_soeknad
  )?.opplysning

  const gjenlevendePdl: IPersonOpplysningFraPdl = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.gjenlevende_forelder_pdl
  )?.opplysning

  const gjenlevendeSoknad: IGjenlevendeFraSoeknad = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.gjenlevende_forelder_soeknad
  )?.opplysning

  const mottattDato: string = grunnlag.find((g) => g.opplysningType === OpplysningsType.soeknad_mottatt)?.opplysning
    ?.mottattDato

  const innsender: IPersonOpplysning = grunnlag.find((g) => g.opplysningType === OpplysningsType.innsender)?.opplysning

  //TODO regne ut virkningstidspunkt i backend?
  const virkningstidspunkt: string = hentVirkningstidspunkt(avdoedPersonPdl?.doedsdato, mottattDato)

  return {
    soekerPdl,
    soekerSoknad,
    avdoedPersonPdl,
    avdodPersonSoknad,
    gjenlevendePdl,
    gjenlevendeSoknad,
    mottattDato,
    innsender,
    virkningstidspunkt,
  }
}
