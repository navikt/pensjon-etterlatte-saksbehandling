import { Filter } from '~components/oppgavebenk/filter/oppgavelistafiltre'

export const FILTER_KEY = 'filter'

export const leggFilterILocalStorage = (filter: Filter) => {
  localStorage.setItem(FILTER_KEY, JSON.stringify(filter))
}

export const hentFilterFraLocalStorage = (): Filter => {
  return JSON.parse(localStorage[FILTER_KEY])
}

export const eksistererFilterILocalStorage = (): boolean => !!localStorage[FILTER_KEY]
