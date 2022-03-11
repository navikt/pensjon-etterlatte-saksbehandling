import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'

import { KildeType, OpplysningsType } from '../../../store/reducers/BehandlingReducer'

export const usePersonInfoFromBehandling = () => {
  const ctx = useContext(AppContext)

  const grunnlag = ctx.state.behandlingReducer.grunnlag

  console.log('usePersonInfoHook', grunnlag)
  /*
    Todo: Dra ut i behandlingsHook
  */
  const soekerPdl: any = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_personinfo && g.kilde.type === KildeType.pdl
  )
  const soekerSoknad: any = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_personinfo && g.kilde.type === KildeType.privatperson
  )
  const avdodPersonPdl: any = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_personinfo && g.kilde.type === KildeType.pdl
  )
  const avdodPersonSoknad: any = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_personinfo && g.kilde.type === KildeType.privatperson
  )
  const mottattDato = grunnlag.find((g) => g.opplysningType === OpplysningsType.soeknad_mottatt)
  const sosken = grunnlag.find((g) => g.opplysningType === OpplysningsType.soeker_relasjon_soeksken)
  const soekerFoedseldato = grunnlag.find((g) => g.opplysningType === OpplysningsType.soeker_foedselsdato)

  const dodsfall = grunnlag.find((g) => g.opplysningType === OpplysningsType.avdoed_doedsfall)
  const innsender = grunnlag.find((g) => g.opplysningType === OpplysningsType.innsender)
  const gjenlevendePdl = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.gjenlevende_forelder_personinfo && g.kilde.type === KildeType.pdl
  )
  const gjenlevendeSoknad = grunnlag.find(
    (g) =>
      g.opplysningType === OpplysningsType.gjenlevende_forelder_personinfo && g.kilde.type === KildeType.privatperson
  )

  const soekerBostedadresserPdl = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_bostedadresse && g.kilde.type === KildeType.pdl
  )

  const avdoedBostedadresserPdl = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_bostedadresse && g.kilde.type === KildeType.pdl
  )

  const gjenlevendeBostedadresserPdl = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.gjenlevende_forelder_bostedsadresse && g.kilde.type === KildeType.pdl
  )

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
    soekerFoedseldato
  }
}
