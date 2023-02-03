import { format } from 'date-fns'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'

export const capitalize = (s?: string) => {
  if (!s) return ''
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase()
}

export const formaterEnumTilLesbarString = (string: string): string => {
  const storForbokstav = capitalize(string.toLowerCase())
  return storForbokstav.replace('_', ' ')
}

export const formaterDato = (dato: Date) => format(dato, 'dd.MM.yyyy').toString()

export const formaterStringDato = (dato: string) => format(new Date(dato), 'dd.MM.yyyy').toString()
export const formaterKanskjeStringDato = (dato?: string): string =>
  formaterKanskjeStringDatoMedFallback('Ukjent dato', dato)

export const formaterKanskjeStringDatoMedFallback = (fallback: string, dato?: string): string =>
  dato ? formaterStringDato(dato) : fallback

export const formaterTidspunkt = (dato: Date) => format(dato, 'HH:mm').toString()

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
