import { Filter, initialFilter } from '~components/oppgavebenk/filter/oppgavelistafiltre'

export const FILTER_KEY = 'filter'

export const leggFilterILocalStorage = (filter: Filter) => {
  localStorage.setItem(FILTER_KEY, JSON.stringify(filter))
}

export const hentFilterFraLocalStorage = (): Filter => {
  const filter = localStorage[FILTER_KEY]
  if (!!filter) return JSON.parse(filter)
  else return initialFilter()
}
