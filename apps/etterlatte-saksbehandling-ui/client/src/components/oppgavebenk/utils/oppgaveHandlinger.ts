import { logger } from '~utils/logger'
import { OppgaveDTO, OppgaveSaksbehandler, Oppgavestatus } from '~shared/types/oppgave'

export const finnOgOppdaterOppgave = (
  oppgaver: OppgaveDTO[],
  oppgaveId: string,
  felter: {
    status?: Oppgavestatus
    frist?: string
    saksbehandler?: OppgaveSaksbehandler | null
  }
) => {
  const { status, frist, saksbehandler } = felter
  const index = oppgaver.findIndex((o) => o.id === oppgaveId)
  if (index > -1) {
    const oppdatertOppgaveState = [...oppgaver]
    if (status) oppdatertOppgaveState[index].status = status
    if (frist) oppdatertOppgaveState[index].frist = frist
    if (saksbehandler) oppdatertOppgaveState[index].saksbehandler = saksbehandler
    return oppdatertOppgaveState
  } else {
    return oppgaver
  }
}

export const leggTilOppgavenIMinliste = (
  oppgaver: OppgaveDTO[],
  oppgave: OppgaveDTO,
  saksbehandler: OppgaveSaksbehandler | null
): OppgaveDTO[] => {
  return oppgaver.concat({
    ...oppgave,
    saksbehandler: saksbehandler,
    status: Oppgavestatus.UNDER_BEHANDLING,
  })
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
    logger.generalError({ msg: 'Feil i paginering av sortering fra localstorage' })
    return DEFAULT_PAGINERING_SIZE
  }
}

function isNumeric(value: string) {
  return /^-?\d+$/.test(value)
}
