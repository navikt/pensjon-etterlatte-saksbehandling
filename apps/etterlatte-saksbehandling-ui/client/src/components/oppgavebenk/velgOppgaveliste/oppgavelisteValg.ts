import { logger } from '~utils/logger'

export enum OppgavelisteValg {
  OPPGAVELISTA = 'Oppgavelista',
  MIN_OPPGAVELISTE = 'MinOppgaveliste',
  GOSYS_OPPGAVER = 'GosysOpggaver',
}

export const TAB_NUMMER: Record<OppgavelisteValg, string> = {
  [OppgavelisteValg.OPPGAVELISTA]: '1',
  [OppgavelisteValg.MIN_OPPGAVELISTE]: '2',
  [OppgavelisteValg.GOSYS_OPPGAVER]: '3',
}

export const NUMMER_TIL_TAB: Record<string, OppgavelisteValg> = {
  '1': OppgavelisteValg.OPPGAVELISTA,
  '2': OppgavelisteValg.MIN_OPPGAVELISTE,
  '3': OppgavelisteValg.GOSYS_OPPGAVER,
}

const OPPGAVELISTE_VALG = 'oppgaveliste'

const initialValg = OppgavelisteValg.OPPGAVELISTA

export const leggValgILocalstorage = (valg: string) => localStorage.setItem(OPPGAVELISTE_VALG, valg)

export const hentValgFraLocalStorage = (): string => {
  try {
    const valg = localStorage[OPPGAVELISTE_VALG]
    if (!!valg) return valg
    else return initialValg
  } catch {
    logger.generalError({ msg: 'Feil i hentingen av oppgavelista valg fra localstorage' })

    return initialValg
  }
}
