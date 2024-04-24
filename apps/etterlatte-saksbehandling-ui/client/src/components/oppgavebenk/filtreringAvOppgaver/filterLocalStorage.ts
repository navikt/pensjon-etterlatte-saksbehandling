import { initialFilter } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { logger } from '~utils/logger'
import { Filter, GosysFilter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'

const FILTER_KEY = 'filter'
const GOSYS_FILTER_KEY = 'gosysFilter'

export const leggFilterILocalStorage = (filter: Filter) => localStorage.setItem(FILTER_KEY, JSON.stringify(filter))

export const leggGosysFilterILocalStorage = (filter: GosysFilter) => {
  localStorage.setItem(GOSYS_FILTER_KEY, JSON.stringify(filter))
}

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

export const hentGosysFilterFraLocalStorage = (): GosysFilter => {
  try {
    const filter = localStorage[GOSYS_FILTER_KEY]
    if (!!filter) return JSON.parse(filter)
    else return {}
  } catch (error) {
    logger.generalError({ message: 'Feil i hentingen av gosys filter fra localstorage' })

    return {}
  }
}
