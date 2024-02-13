import { Filter, initialFilter } from '~components/oppgavebenk/filter/oppgavelistafiltre'
import { logger } from '~utils/logger'

const FILTER_KEY = 'filter'

export const leggFilterILocalStorage = (filter: Filter) => localStorage.setItem(FILTER_KEY, JSON.stringify(filter))

export const hentFilterFraLocalStorage = (): Filter => {
  try {
    const filterFraLocalStorage = localStorage[FILTER_KEY]
    if (!!filterFraLocalStorage) {
      return JSON.parse(filterFraLocalStorage)
    } else {
      return initialFilter()
    }
  } catch (error) {
    logger.generalError({ message: 'Feil i hentingen av filter fra localstorage' })

    return initialFilter()
  }
}
