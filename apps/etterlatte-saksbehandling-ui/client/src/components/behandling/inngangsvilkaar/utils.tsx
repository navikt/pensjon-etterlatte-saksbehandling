import { IAdresse } from '../../../store/reducers/BehandlingReducer'
import { isAfter } from 'date-fns'

import { IGap, IReturnertPeriodeType, ITidslinjePeriode, IVurdertPeriode, TidslinjePeriodeType } from './types'

export function mapAdresseTilPerioderSeksAarFoerDoedsdato(
  adresser: IAdresse[] | undefined,
  periodetype: string,
  seksAarFoerDoedsdato: string,
  kilde: any
): ITidslinjePeriode[] {
  if (adresser == null || adresser.length == 0) {
    return []
  }

  const seksAarFoerDoedsdatoEllerAktiv = adresser.filter(
    (adresse) => adresse.aktiv || isAfter(new Date(adresse.gyldigTilOgMed!), new Date(seksAarFoerDoedsdato))
  )

  function mapTilPeriode(adresse: IAdresse): ITidslinjePeriode {
    return {
      type: TidslinjePeriodeType.ADRESSE,
      innhold: {
        typeAdresse: periodetype,
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

export function mapYtelseTilPerioderSeksAarFoerDoedsdato(
  perioder: IVurdertPeriode[] | undefined,
  seksAarFoerDoedsdato: string
): ITidslinjePeriode[] {
  if (perioder == null || perioder.length == 0) {
    return []
  }

  const seksAarFoerDoedsdatoEllerAktiv = perioder.filter((periode) =>
    isAfter(new Date(periode.tilDato), new Date(seksAarFoerDoedsdato))
  )

  function mapTilPeriode(periode: IVurdertPeriode): ITidslinjePeriode {
    return {
      type:
        periode.periodeType === IReturnertPeriodeType.arbeidsperiode
          ? TidslinjePeriodeType.ARBEIDSPERIODE
          : TidslinjePeriodeType.INNTEKT,
      innhold: periode,
      kilde: periode.kilde.type,
    }
  }

  return seksAarFoerDoedsdatoEllerAktiv
    ?.map((periode: IVurdertPeriode) => mapTilPeriode(periode))
    .sort((a, b) => (new Date(b.innhold.fraDato) < new Date(a.innhold.fraDato) ? 1 : -1))
}

export function mapGapsTilPerioder(gaps: IGap[], kilde: any): ITidslinjePeriode[] {
  if (gaps == null || gaps.length == 0) {
    return []
  }

  function mapTilPeriode(gap: IGap): ITidslinjePeriode {
    return {
      type: TidslinjePeriodeType.GAP,
      innhold: {
        typeAdresse: '',
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
