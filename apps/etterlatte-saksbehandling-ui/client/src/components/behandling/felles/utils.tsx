import { isAfter } from 'date-fns'
import {
  IAdresse,
  IKriterie,
  IKriterieOpplysning,
  IVilkaarsproving,
  KriterieOpplysningsType,
  Kriterietype,
} from '../../../store/reducers/BehandlingReducer'
import { Periode } from '../inngangsvilkaar/vilkaar/TidslinjeMedlemskap'

export function hentAdresserEtterDoedsdato(adresser: IAdresse[], doedsdato: string | null): IAdresse[] {
  if (doedsdato == null) {
    return adresser
  }
  return adresser?.filter((adresse) => adresse.aktiv || isAfter(new Date(adresse.gyldigTilOgMed!), new Date(doedsdato)))
}

export function mapTilPerioderSeksAarFoerDoedsdato(
  adresser: IAdresse[] | undefined,
  periodetype: string,
  seksAarFoerDoedsdato: string,
  kilde: any
): Periode[] {
  if (adresser == null || adresser.length == 0) {
    return []
  }
  const seksAarFoerDoedsdatoEllerAktiv = adresser.filter(
    (adresse) => adresse.aktiv || isAfter(new Date(adresse.gyldigTilOgMed!), new Date(seksAarFoerDoedsdato))
  )

  function mapTilPeriode(adresse: IAdresse) {
    return {
      periodeType: periodetype,
      innhold: {
        fraDato: adresse.gyldigFraOgMed,
        tilDato: adresse.gyldigTilOgMed,
        beskrivelse: adresse.adresseLinje1 + ', ' + adresse.postnr + ' ' + (adresse.poststed ? adresse.poststed : ''),
        adresseINorge: adresse.type != 'UTENLANDSKADRESSE' && adresse.type != 'UTENLANDSKADRESSEFRITTFORMAT',
        land: adresse.land,
      },
      kilde: kilde,
    }
  }

  return seksAarFoerDoedsdatoEllerAktiv?.map((adresse: IAdresse) => mapTilPeriode(adresse))
}

export function hentUtenlandskAdresse(adresser: IAdresse[], doedsdato: string | null): IAdresse[] {
  const etterDoedsdato = hentAdresserEtterDoedsdato(adresser, doedsdato)
  return etterDoedsdato?.filter((ad) => ad.type === 'UTENLANDSKADRESSE' || ad.type === 'UTENLANDSKADRESSEFRITTFORMAT')
}

export const hentKriterie = (
  vilkaar: IVilkaarsproving | undefined,
  kriterieType: Kriterietype
): IKriterie | undefined => {
  return vilkaar?.kriterier?.find((krit: IKriterie) => krit.navn === kriterieType)
}

export const hentKriterieOpplysning = (
  krit: IKriterie | undefined,
  kriterieOpplysningsType: KriterieOpplysningsType
): IKriterieOpplysning | undefined => {
  return krit?.basertPaaOpplysninger.find(
    (opplysning: IKriterieOpplysning) => opplysning.kriterieOpplysningsType === kriterieOpplysningsType
  )
}

export const hentKriterierMedOpplysning = (
  vilkaar: IVilkaarsproving | undefined,
  kriterieType: Kriterietype,
  kriterieOpplysningsType: KriterieOpplysningsType
): IKriterieOpplysning | undefined => {
  try {
    return vilkaar?.kriterier
      ?.find((krit: IKriterie) => krit.navn === kriterieType)
      ?.basertPaaOpplysninger.find(
        (opplysning: IKriterieOpplysning) => opplysning.kriterieOpplysningsType === kriterieOpplysningsType
      )
  } catch (e: any) {
    console.error(e)
  }
}
