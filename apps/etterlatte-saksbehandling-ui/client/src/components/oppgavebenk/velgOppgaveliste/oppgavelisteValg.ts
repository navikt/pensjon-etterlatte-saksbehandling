import { logger } from '~utils/logger'

export enum OppgavelisteValg {
  OPPGAVELISTA = 'Oppgavelista',
  MIN_OPPGAVELISTE = 'MinOppgaveliste',
  GOSYS_OPPGAVER = 'GosysOpggaver',
}

const OPPGAVELISTE_VALG_KEY = 'oppgaveliste'

const initialValg = OppgavelisteValg.OPPGAVELISTA

export const leggValgILocalstorage = (valg: string) => localStorage.setItem(OPPGAVELISTE_VALG_KEY, valg)

export const hentValgFraLocalStorage = (): string => {
  try {
    const valg = localStorage[OPPGAVELISTE_VALG_KEY]
    if (!!valg) return valg
    else return initialValg
  } catch (error) {
    logger.generalError({ msg: 'Feil i hentingen av oppgavelista valg fra localstorage' })

    return initialValg
  }
}
