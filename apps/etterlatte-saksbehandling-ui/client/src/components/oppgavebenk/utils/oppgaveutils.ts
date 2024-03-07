import { OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import { logger } from '~utils/logger'

export const finnOgOppdaterSaksbehandlerTildeling = (
  oppgaver: OppgaveDTO[],
  oppgaveId: string,
  saksbehandler: OppgaveSaksbehandler | null,
  versjon: number | null
) => {
  const index = oppgaver.findIndex((o) => o.id === oppgaveId)
  if (index > -1) {
    const oppdatertOppgaveState = [...oppgaver]
    oppdatertOppgaveState[index].saksbehandler = saksbehandler
    oppdatertOppgaveState[index].status = 'UNDER_BEHANDLING'
    oppdatertOppgaveState[index].versjon = versjon
    return oppdatertOppgaveState
  } else {
    return oppgaver
  }
}

export const leggTilOppgavenIMinliste = (
  oppgaver: OppgaveDTO[],
  oppgave: OppgaveDTO,
  saksbehandler: OppgaveSaksbehandler | null,
  versjon: number | null
): OppgaveDTO[] => {
  return [...oppgaver, { ...oppgave, saksbehandler: saksbehandler, status: 'UNDER_BEHANDLING', versjon: versjon }]
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

export interface OppgavelisteneStats {
  antallOppgavelistaOppgaver: number
  antallMinOppgavelisteOppgaver: number
  antallGosysOppgaver: number
}

export const initalOppgavelisteneStats: OppgavelisteneStats = {
  antallOppgavelistaOppgaver: 0,
  antallMinOppgavelisteOppgaver: 0,
  antallGosysOppgaver: 0,
}
