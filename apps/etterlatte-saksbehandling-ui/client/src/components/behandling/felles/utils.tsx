import { isAfter } from 'date-fns'
import {
  IAdresse,
  IBehandlingStatus,
  IKriterie,
  IKriterieOpplysning,
  IVilkaarsproving,
  KriterieOpplysningsType,
  Kriterietype,
} from '../../../store/reducers/BehandlingReducer'
import { IGap, IPeriode } from '../inngangsvilkaar/vilkaar/TidslinjeMedlemskap'

export function hentAdresserEtterDoedsdato(adresser: IAdresse[], doedsdato: string | null): IAdresse[] {
  if (doedsdato == null) {
    return adresser
  }
  return adresser?.filter((adresse) => adresse.aktiv || isAfter(new Date(adresse.gyldigTilOgMed!), new Date(doedsdato)))
}

export function mapAdresseTilPerioderSeksAarFoerDoedsdato(
  adresser: IAdresse[] | undefined,
  periodetype: string,
  seksAarFoerDoedsdato: string,
  kilde: any
): IPeriode[] {
  if (adresser == null || adresser.length == 0) {
    return []
  }

  const seksAarFoerDoedsdatoEllerAktiv = adresser.filter(
    (adresse) => adresse.aktiv || isAfter(new Date(adresse.gyldigTilOgMed!), new Date(seksAarFoerDoedsdato))
  )

  function mapTilPeriode(adresse: IAdresse): IPeriode {
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

  return seksAarFoerDoedsdatoEllerAktiv
    ?.map((adresse: IAdresse) => mapTilPeriode(adresse))
    .sort((a, b) => (new Date(b.innhold.fraDato) < new Date(a.innhold.fraDato) ? 1 : -1))
}

export function mapGapsTilPerioder(gaps: IGap[], kilde: any): IPeriode[] {
  if (gaps == null || gaps.length == 0) {
    return []
  }

  function mapTilPeriode(gap: IGap): IPeriode {
    return {
      periodeType: 'Bostedgap',
      innhold: {
        fraDato: gap.gyldigFra,
        tilDato: gap.gyldigTil,
        beskrivelse: '',
        adresseINorge: true,
        land: undefined,
      },
      kilde: kilde,
    }
  }

  return gaps?.map((gap: IGap) => mapTilPeriode(gap))
}

export function hentUtenlandskAdresseEtterDoedsdato(adresser: IAdresse[], doedsdato: string | null): IAdresse[] {
  const etterDoedsdato = hentAdresserEtterDoedsdato(adresser, doedsdato)
  return etterDoedsdato?.filter((ad) => ad.type === 'UTENLANDSKADRESSE' || ad.type === 'UTENLANDSKADRESSEFRITTFORMAT')
}

export function hentUtlandskeAdresser(adresser: IAdresse[] | undefined): IAdresse[] {
  const utlandsadresser = adresser?.filter(
    (ad) => ad.type === 'UTENLANDSKADRESSE' || ad.type === 'UTENLANDSKADRESSEFRITTFORMAT'
  )
  return utlandsadresser ? utlandsadresser : []
}

export function hentNorskeAdresser(adresser: IAdresse[] | undefined): IAdresse[] {
  const norske = adresser?.filter((ad) => ad.type !== 'UTENLANDSKADRESSE' && ad.type !== 'UTENLANDSKADRESSEFRITTFORMAT')
  return norske ? norske : []
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

export const hentBehandlesFraStatus = (status: IBehandlingStatus): boolean => {
  return (
    status === IBehandlingStatus.UNDER_BEHANDLING ||
    status === IBehandlingStatus.GYLDIG_SOEKNAD ||
    status === IBehandlingStatus.RETURNERT
  )
}
