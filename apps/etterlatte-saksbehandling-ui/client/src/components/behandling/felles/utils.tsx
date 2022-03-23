import { IAdresse } from '../types'
import moment from 'moment'
import {
  IKriterie,
  IKriterieOpplysning,
  KriterieOpplysningsType,
  Kriterietype,
} from '../../../store/reducers/BehandlingReducer'

export function hentAdresserEtterDoedsdato(adresser: IAdresse[], doedsdato: Date): IAdresse[] {
  const etterDoedsdatoEllerAktiv = adresser?.filter(
    (adresse) => moment(adresse.gyldigTilOgMed).isAfter(moment(doedsdato)) || adresse.aktiv
  )

  return etterDoedsdatoEllerAktiv
}

export function hentUtenlandskAdresse(adresser: IAdresse[], doedsdato: Date): IAdresse[] {
  const etterDoedsdato = hentAdresserEtterDoedsdato(adresser, doedsdato)
  return etterDoedsdato?.filter((ad) => ad.type === 'UTENLANDSKADRESSE' || ad.type === 'UTENLANDSKADRESSEFRITTFORMAT')
}

/*
TODO: type opp return type
*/

export const hentKriterie = (vilkaar: any, kriterieType: Kriterietype) => {
  return vilkaar.kriterier?.find((krit: IKriterie) => krit.navn === kriterieType)
}

export const hentKriterieOpplysning = (krit: IKriterie, kriterieOpplysningsType: KriterieOpplysningsType) => {
  return krit?.basertPaaOpplysninger.find(
    (opplysning: IKriterieOpplysning) => opplysning.kriterieOpplysningsType === kriterieOpplysningsType
  )
}

export const hentKriterierMedOpplysning = (
  vilkaar: any,
  kriterieType: Kriterietype,
  kriterieOpplysningsType: KriterieOpplysningsType
) => {
  try {
    return vilkaar.kriterier
      ?.find((krit: IKriterie) => krit.navn === kriterieType)
      ?.basertPaaOpplysninger.find(
        (opplysning: IKriterieOpplysning) => opplysning.kriterieOpplysningsType === kriterieOpplysningsType
      )
  } catch (e: any) {
    console.error(e)
  }
}
