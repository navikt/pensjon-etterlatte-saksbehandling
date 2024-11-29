import { add } from 'date-fns'

export const datoIMorgen = (): Date => {
  return add(new Date(), { days: 1 })
}

export const datoToAarFramITid = (): Date => {
  return add(new Date(), { years: 2 })
}
