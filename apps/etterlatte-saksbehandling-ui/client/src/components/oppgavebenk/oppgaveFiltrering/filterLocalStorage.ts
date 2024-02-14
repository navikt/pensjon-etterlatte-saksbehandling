import { Filter, initialFilter } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import { logger } from '~utils/logger'

const FILTER_KEY = 'filter'

export const leggFilterILocalStorage = (filter: Filter) => localStorage.setItem(FILTER_KEY, JSON.stringify(filter))

export const hentFilterFraLocalStorage = (): Filter => {
  try {
    const filterFraLocalStorage = localStorage[FILTER_KEY]

    if (!!filterFraLocalStorage) {
      const parsetFilter = JSON.parse(filterFraLocalStorage)
      // TODO: quickfix etter endring i type i localstorage, fjernes etter at
      //  en viss tid, slik at alle saksbehandlere har kommet seg bort fra den gamel typen
      return {
        ...parsetFilter,
        fristSortering: parsetFilter.fristSortering === 'ingen' ? 'no-order' : parsetFilter.fristSortering,
        fnrSortering: parsetFilter.fnrSortering === 'ingen' ? 'no-order' : parsetFilter.fnrSortering,
      }
    } else {
      return initialFilter()
    }
  } catch (error) {
    logger.generalError({ message: 'Feil i hentingen av filter fra localstorage' })

    return initialFilter()
  }
}
