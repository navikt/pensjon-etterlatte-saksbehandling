import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'

import { KildeType, OpplysningsType } from '../../../store/reducers/BehandlingReducer'
import { IAdresse, IDodsfall, IPersonOpplysning } from './types'

export const usePersonInfoFromBehandling = () => {
  const ctx = useContext(AppContext)

  const grunnlag = ctx.state.behandlingReducer.grunnlag

  console.log('usePersonInfoHook', grunnlag)
  /*
    Todo: Dra ut i behandlingsHook
  */
  const soekerPdl: IPersonOpplysning = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_personinfo && g.kilde.type === KildeType.pdl
  )?.opplysning

  const soekerSoknad: IPersonOpplysning = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_personinfo && g.kilde.type === KildeType.privatperson
  )?.opplysning

  const avdodPersonPdl: IPersonOpplysning = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_personinfo && g.kilde.type === KildeType.pdl
  )?.opplysning

  const avdodPersonSoknad: IPersonOpplysning = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_personinfo && g.kilde.type === KildeType.privatperson
  )?.opplysning

  const mottattDato = grunnlag.find((g) => g.opplysningType === OpplysningsType.soeknad_mottatt)?.opplysning

  const sosken = grunnlag.find((g) => g.opplysningType === OpplysningsType.soeker_relasjon_soeksken)

  const soekerFoedseldato = grunnlag.find((g) => g.opplysningType === OpplysningsType.soeker_foedselsdato)?.opplysning
    .foedselsdato

  const dodsfall: IDodsfall = grunnlag.find((g) => g.opplysningType === OpplysningsType.avdoed_doedsfall)?.opplysning

  const innsender = grunnlag.find((g) => g.opplysningType === OpplysningsType.innsender)?.opplysning

  const gjenlevendePdl: IPersonOpplysning = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.gjenlevende_forelder_personinfo && g.kilde.type === KildeType.pdl
  )?.opplysning

  const gjenlevendeSoknad: IPersonOpplysning = grunnlag.find(
    (g) =>
      g.opplysningType === OpplysningsType.gjenlevende_forelder_personinfo && g.kilde.type === KildeType.privatperson
  )?.opplysning

  const soekerBostedadresserPdl: IAdresse[] = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_bostedadresse && g.kilde.type === KildeType.pdl
  )?.opplysning.bostedadresse

  const avdoedBostedadresserPdl: IAdresse[] = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_bostedadresse && g.kilde.type === KildeType.pdl
  )?.opplysning.bostedadresse

  const gjenlevendeBostedadresserPdl: IAdresse[] = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.gjenlevende_forelder_bostedsadresse && g.kilde.type === KildeType.pdl
  )?.opplysning.bostedadresse

  const gjenlevendeForelderInfo: any = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.gjenlevende_forelder_info
  )?.opplysning

  return {
    soekerSoknad,
    soekerPdl,
    avdodPersonPdl,
    avdodPersonSoknad,
    mottattDato,
    sosken,
    dodsfall,
    innsender,
    gjenlevendePdl,
    gjenlevendeSoknad,
    soekerBostedadresserPdl,
    avdoedBostedadresserPdl,
    gjenlevendeBostedadresserPdl,
    soekerFoedseldato,
    gjenlevendeForelderInfo,
  }
}
