import { IAdresse } from '../soeknadsoversikt/types'
import moment from 'moment'
import { IKriterie, IVilkaaropplysing, Kriterietype, OpplysningsType } from '../../../store/reducers/BehandlingReducer'

export function hentAdresserEtterDoedsdato(adresser: IAdresse[], doedsdato: Date): IAdresse[] {
  const etterDoedsdatoEllerAktiv = adresser?.filter(
    (adresse) => moment(adresse.gyldigTilOgMed).isAfter(moment(doedsdato)) || adresse.aktiv
  )

  return etterDoedsdatoEllerAktiv
}

/*
TODO: type opp return type
*/

export const hentKriterier = (vilkaar: any, kriterieType: Kriterietype, opplysningsType: OpplysningsType) => {
  try {
    return vilkaar.kriterier
      ?.find((krit: IKriterie) => krit.navn === kriterieType)
      ?.basertPaaOpplysninger.find((opplysning: IVilkaaropplysing) => opplysning.opplysningsType === opplysningsType)
  } catch (e: any) {
    console.error(e)
  }
}
