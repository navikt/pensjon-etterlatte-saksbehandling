import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { OpplysningsType } from '../inngangsvilkaar/types'
import { KildeType } from '../../../store/reducers/BehandlingReducer'


export const usePersonInfoFromBehandling = () => {
  const ctx = useContext(AppContext)

  const grunnlag = ctx.state.behandlingReducer.grunnlag

  console.log("usePersonInfoHook", grunnlag)
  /*
    Todo: Dra ut i behandlingsHook 
  */
  const soekerPdl: any = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_personinfo && g.kilde.type === KildeType.pdl
  )
  /*
  const soekerSoknad: any = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.soeker_personinfo && g.kilde.type === KildeType.privatperson
  )
  */
  const avdodPersonPdl: any = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_personinfo && g.kilde.type === KildeType.pdl
  )
  const avdodPersonSoknad: any = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.avdoed_personinfo && g.kilde.type === KildeType.privatperson
  )
  const mottattDato = grunnlag.find((g) => g.opplysningType === OpplysningsType.soeknad_mottatt)
  const sosken = grunnlag.find((g) => g.opplysningType === OpplysningsType.relasjon_soksken)
  const dodsfall = grunnlag.find((g) => g.opplysningType === OpplysningsType.avdoed_doedsfall)
  const innsender = grunnlag.find((g) => g.opplysningType === OpplysningsType.innsender)
  const gjenlevendePdl = grunnlag.find(
    (g) => g.opplysningType === OpplysningsType.gjenlevende_forelder_personinfo && g.kilde.type === KildeType.pdl
  )
  const gjenlevendeSoknad = grunnlag.find(
    (g) =>
      g.opplysningType === OpplysningsType.gjenlevende_forelder_personinfo && g.kilde.type === KildeType.privatperson
  )

  return {
    soekerPdl,
    avdodPersonPdl,
    avdodPersonSoknad,
    mottattDato,
    sosken,
    dodsfall,
    innsender,
    gjenlevendePdl,
    gjenlevendeSoknad
  }
}
