import moment from 'moment'


export const upperCaseFirst = (string: string): string => {
    return string.charAt(0).toUpperCase() + string.slice(1);
}

export const formatterDato = (dato: Date) => {
  return moment(dato).format('DD.MM.YYYY').toString()
}

export const formatterTidspunkt = (dato: Date) => {
  return moment(dato).format('HH:mm').toString()
}
