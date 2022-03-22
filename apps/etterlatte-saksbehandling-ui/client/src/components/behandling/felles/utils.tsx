import { IAdresse } from '../types'
import moment from 'moment'
import {
  IKriterie,
  IKriterieOpplysing,
  KriterieOpplysningsType,
  Kriterietype,
  OpplysningsType,
} from '../../../store/reducers/BehandlingReducer'

export function hentAdresserEtterDoedsdato(adresser: IAdresse[], doedsdato: Date): IAdresse[] {
  const etterDoedsdatoEllerAktiv = adresser?.filter(
    (adresse) => moment(adresse.gyldigTilOgMed).isAfter(moment(doedsdato)) || adresse.aktiv
  )

  return etterDoedsdatoEllerAktiv
}

/*
TODO: type opp return type
*/

export const hentKriterier = (
  vilkaar: any,
  kriterieType: Kriterietype,
  kriterieOpplysningsType: KriterieOpplysningsType
) => {
  try {
    return vilkaar.kriterier
      ?.find((krit: IKriterie) => krit.navn === kriterieType)
      ?.basertPaaOpplysninger.find(
        (opplysning: IKriterieOpplysing) => opplysning.kriterieOpplysningsType === kriterieOpplysningsType
      )
  } catch (e: any) {
    console.error(e)
  }
}
