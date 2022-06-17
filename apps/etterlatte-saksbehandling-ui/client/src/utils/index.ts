import { format } from 'date-fns'

export const upperCaseFirst = (string: string): string => {
  return string.charAt(0).toUpperCase() + string.slice(1)
}

export const formatterDato = (dato: Date) => {
  return format(dato, 'dd.MM.yyyy').toString()
}

export const formatterStringDato = (dato: string) => {
  return format(new Date(dato), 'dd.MM.yyyy').toString()
}

export const formatterTidspunkt = (dato: Date) => {
  return format(dato, 'HH:mm').toString()
}
