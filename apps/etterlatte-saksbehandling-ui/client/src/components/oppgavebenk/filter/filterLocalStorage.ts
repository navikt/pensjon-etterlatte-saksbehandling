import { Filter, initialFilter } from '~components/oppgavebenk/filter/oppgavelistafiltre'

export const FILTER_KEY = 'filter'

export const leggFilterILocalStorage = (filter: Filter) => {
  localStorage.setItem(FILTER_KEY, JSON.stringify(filter))
}

export const hentFilterFraLocalStorage = (): Filter => {
  const filter = localStorage[FILTER_KEY]
  if (!!filter) {
    const parsetFilter = JSON.parse(filter)
    // TODO: quickfix etter endring i type i localstorage, fjernes etter at
    //  en viss tid, slik at alle saksbehandlere har kommet seg bort fra den gamel typen
    return {
      ...parsetFilter,
      fristFilter: parsetFilter.fristFilter === 'ingen' ? 'no-order' : parsetFilter.fristFilter,
      fnrFilter: parsetFilter.fnrFilter === 'ingen' ? 'no-order' : parsetFilter.fnrFilter,
    }
  } else return initialFilter()
}
