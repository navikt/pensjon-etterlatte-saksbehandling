import { format } from 'date-fns'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { VedtakType } from '~components/vedtak/typer'

export const capitalize = (s?: string) => {
  if (!s) return ''
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase()
}

export const formaterEnumTilLesbarString = (string: string): string => {
  const storForbokstav = capitalize(string.toLowerCase())
  return storForbokstav.replace('_', ' ')
}

export const formaterDatoTilYearMonth = (dato: Date | null) => (dato ? format(dato, 'yyyy-MM').toString() : '')

export const formaterDato = (dato: Date) => format(dato, 'dd.MM.yyyy').toString()

export const formaterStringDato = (dato: string) => format(new Date(dato), 'dd.MM.yyyy').toString()
export const formaterKanskjeStringDato = (dato?: string): string =>
  formaterKanskjeStringDatoMedFallback('Ukjent dato', dato)

export const formaterKanskjeStringDatoMedFallback = (fallback: string, dato?: string): string =>
  dato ? formaterStringDato(dato) : fallback

export const formaterStringTidspunkt = (dato: string) => format(new Date(dato), 'HH:mm').toString()

export const formaterDatoMedTidspunkt = (dato: Date) => format(new Date(dato), 'dd.MM.yyyy HH:mm').toString()

export const formaterBehandlingstype = (behandlingstype: IBehandlingsType): string => {
  switch (behandlingstype) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return 'Førstegangsbehandling'
    case IBehandlingsType.REVURDERING:
      return 'Revurdering'
    case IBehandlingsType.MANUELT_OPPHOER:
      return 'Manuelt opphør'
  }
}

export const formaterSakstype = (sakstype: SakType): string => {
  switch (sakstype) {
    case SakType.BARNEPENSJON:
      return 'Barnepensjon'
    case SakType.OMSTILLINGSSTOENAD:
      return 'Omstillingsstønad'
  }
}

export const formaterVedtakType = (type: VedtakType): string => {
  switch (type) {
    case VedtakType.INNVILGELSE:
      return 'Innvilgelse'
    case VedtakType.OPPHOER:
      return 'Opphør'
    case VedtakType.AVSLAG:
      return 'Avslag'
    case VedtakType.ENDRING:
      return 'Endring'
  }
}

export const formaterFnr = (fnr: String) => {
  if (fnr.length === 11) return fnr.replace(/\d{6}(?=.)/g, '$& ')
  return fnr
}

export enum DatoFormat {
  AAR_MAANED_DAG = 'yyyy-MM-dd',
  DAG_MAANED_AAR = 'dd.MM.yyyy',
}
const norskKroneFormat = new Intl.NumberFormat('NO-nb', {
  currency: 'nok',
})
export const NOK = (beloep: number | undefined) => (beloep == null ? '' : norskKroneFormat.format(beloep) + ' kr')
