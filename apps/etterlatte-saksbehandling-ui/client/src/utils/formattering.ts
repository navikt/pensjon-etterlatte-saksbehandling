import { format } from 'date-fns'

export const upperCaseFirst = (string: string): string => {
  const lower = string.toLowerCase()
  const storForbokstav = lower.charAt(0).toUpperCase() + lower.slice(1)
  return storForbokstav.replace('_', ' ')
}

export const formaterDato = (dato: Date) => {
  return format(dato, 'dd.MM.yyyy').toString()
}

export const formaterStringDato = (dato: string) => {
  return format(new Date(dato), 'dd.MM.yyyy').toString()
}

export const formaterTidspunkt = (dato: Date) => {
  return format(dato, 'HH:mm').toString()
}

export const formaterStringTidspunkt = (dato: string) => {
  return format(new Date(dato), 'HH:mm').toString()
}
