import { initialFilter } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { logger } from '~utils/logger'
import { Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'

const FILTER_KEY = 'filter'

export const leggFilterILocalStorage = (filter: Filter) => localStorage.setItem(FILTER_KEY, JSON.stringify(filter))

export const hentFilterFraLocalStorage = (): Filter => {
  try {
    const filter = localStorage[FILTER_KEY]
    if (!!filter) return JSON.parse(filter)
    else return initialFilter()
  } catch (error) {
    logger.generalError({ message: 'Feil i hentingen av filter fra localstorage' })

    return initialFilter()
  }
}
