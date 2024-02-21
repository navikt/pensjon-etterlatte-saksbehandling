import { format } from 'date-fns'

export const formatDateToLocaleDateOrEmptyString = (date: Date | undefined) =>
  date === undefined ? '' : format(date, 'yyyy-MM-dd')
