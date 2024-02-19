import { format } from 'date-fns'

export const formatDateToLocaleDateOrEmptyString = (date: Date | undefined) =>
  date === undefined ? '' : format(date, 'yyyy-MM-dd')

export const formatDateToLocalDateTimeOrEmptyString = (date: Date | undefined) =>
  date === undefined ? '' : format(date, "yyyy-MM-dd'T'HH:mm:ss")
