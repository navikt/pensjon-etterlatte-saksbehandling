import { OppgaveDTO } from '~shared/api/oppgaver'
import { logger } from '~utils/logger'

export const oppdaterTildeling =
  (setHentedeOppgaver: (oppdatertListe: OppgaveDTO[]) => void, hentedeOppgaver: OppgaveDTO[]) =>
  (id: string, saksbehandler: string | null, versjon: number | null) => {
    setTimeout(() => {
      const oppdatertOppgaveState = [...hentedeOppgaver]
      const index = oppdatertOppgaveState.findIndex((o) => o.id === id)
      oppdatertOppgaveState[index].saksbehandlerIdent = saksbehandler
      oppdatertOppgaveState[index].status = 'UNDER_BEHANDLING'
      oppdatertOppgaveState[index].versjon = versjon
      setHentedeOppgaver(oppdatertOppgaveState)
    }, 2000)
  }

export const oppdaterFrist = (
  setHentedeOppgaver: (oppdatertListe: OppgaveDTO[]) => void,
  hentedeOppgaver: OppgaveDTO[],
  id: string,
  frist: string,
  versjon: number | null
) => {
  setTimeout(() => {
    const oppdatertOppgaveState = [...hentedeOppgaver]
    const index = oppdatertOppgaveState.findIndex((o) => o.id === id)
    oppdatertOppgaveState[index].frist = frist
    oppdatertOppgaveState[index].versjon = versjon
    setHentedeOppgaver(oppdatertOppgaveState)
  }, 2000)
}

export const sorterOppgaverEtterOpprettet = (oppgaver: OppgaveDTO[]) => {
  return oppgaver.sort((a, b) => new Date(b.opprettet).getTime() - new Date(a.opprettet).getTime())
}

const OPPGAVE_PAGINERING_KEY = 'OPPGAVEPAGINERING'

export const leggTilPagineringLocalStorage = (size: number) =>
  localStorage.setItem(OPPGAVE_PAGINERING_KEY, JSON.stringify(size))

const DEFAULT_PAGINERING_SIZE = 50
export const pagineringslisteverdier = [10, 20, 30, 40, 50]

export const hentPagineringSizeFraLocalStorage = (): number => {
  try {
    const size = localStorage.getItem(OPPGAVE_PAGINERING_KEY)
    if (size && isNumeric(size) && pagineringslisteverdier.includes(Number(size))) {
      return Number(size)
    } else {
      return DEFAULT_PAGINERING_SIZE
    }
  } catch (e) {
    logger.generalError({ message: 'Feil i paginering av sortering fra localstorage' })
    return DEFAULT_PAGINERING_SIZE
  }
}

function isNumeric(value: string) {
  return /^-?\d+$/.test(value)
}
