import { initialFilter } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { logger } from '~utils/logger'
import { Filter, GosysFilter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'

const OPPGAVELISTEN_FILTER_KEY = 'oppgavelistenFilter'
const GOSYS_OPPGAVER_FILTER_KEY = 'gosysOppgaverFilter'

export const leggOppgavelistenFilterILocalStorage = (filter: Filter) =>
  localStorage.setItem(OPPGAVELISTEN_FILTER_KEY, JSON.stringify(filter))

export const leggGosysOppgaverFilterILocalStorage = (filter: GosysFilter) => {
  localStorage.setItem(GOSYS_OPPGAVER_FILTER_KEY, JSON.stringify(filter))
}

export const hentOppgavelistenFilterFraLocalStorage = (): Filter => {
  try {
    const filter = localStorage[OPPGAVELISTEN_FILTER_KEY]
    if (!!filter) return JSON.parse(filter)
    else return initialFilter()
  } catch (error) {
    logger.generalError({ message: 'Feil i hentingen av filter for oppgavelisten fra localstorage' })

    return initialFilter()
  }
}

export const hentGosysOppgaverFilterFraLocalStorage = (): GosysFilter => {
  try {
    const filter = localStorage[GOSYS_OPPGAVER_FILTER_KEY]
    if (!!filter) return JSON.parse(filter)
    else return {}
  } catch (error) {
    logger.generalError({ message: 'Feil i hentingen av gosys filter fra localstorage' })

    return {}
  }
}
