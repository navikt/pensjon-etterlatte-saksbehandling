import { format } from 'date-fns'

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

export const formaterTidspunkt = (dato: Date) => format(dato, 'HH:mm').toString()

export const formaterStringTidspunkt = (dato: string) => format(new Date(dato), 'HH:mm').toString()
