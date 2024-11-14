import {
  initialFilter,
  initialMinOppgavelisteFiltre,
} from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { logger } from '~utils/logger'
import { Filter, GosysFilter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'

const OPPGAVELISTEN_FILTER_KEY = 'oppgavelistenFilter'
const MIN_OPPGAVELISTE_FILTER_KEY = 'minOppgavelisteFilter'
const GOSYS_OPPGAVER_FILTER_KEY = 'gosysOppgaverFilter'

export const leggOppgavelistenFilterILocalStorage = (filter: Filter) => {
  localStorage.setItem(OPPGAVELISTEN_FILTER_KEY, JSON.stringify(filter))
}

export const leggMinOppgavelisteFilterILocalsotrage = (filter: Filter) => {
  localStorage.setItem(MIN_OPPGAVELISTE_FILTER_KEY, JSON.stringify(filter))
}

export const leggGosysOppgaverFilterILocalStorage = (filter: GosysFilter) => {
  localStorage.setItem(GOSYS_OPPGAVER_FILTER_KEY, JSON.stringify(filter))
}

export const hentOppgavelistenFilterFraLocalStorage = (): Filter => {
  try {
    const filter = localStorage[OPPGAVELISTEN_FILTER_KEY]
    if (!!filter) return JSON.parse(filter)
    else return initialFilter()
  } catch {
    logger.generalError({ msg: 'Feil i hentingen av filter for oppgavelisten fra localstorage' })

    return initialFilter()
  }
}

export const hentMinOppgavelisteFilterFraLocalStorage = (): Filter => {
  try {
    const filter = localStorage[MIN_OPPGAVELISTE_FILTER_KEY]
    if (!!filter) return JSON.parse(filter)
    else return initialMinOppgavelisteFiltre()
  } catch {
    logger.generalError({ msg: 'Feil i hentingen av filter for min oppgaveliste fra localstorage' })

    return initialMinOppgavelisteFiltre()
  }
}

export const hentGosysOppgaverFilterFraLocalStorage = (): GosysFilter => {
  try {
    const filter = localStorage[GOSYS_OPPGAVER_FILTER_KEY]
    if (!!filter) return JSON.parse(filter)
    else return {}
  } catch {
    logger.generalError({ msg: 'Feil i hentingen av gosys filter fra localstorage' })

    return {}
  }
}
