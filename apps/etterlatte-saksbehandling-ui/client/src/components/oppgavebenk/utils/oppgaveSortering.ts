import { logger } from '~utils/logger'
import { OppgaveDTO } from '~shared/types/oppgave'
import { GosysOppgave } from '~shared/types/Gosys'

export interface OppgaveSortering {
  registreringsdatoSortering: Retning
  fristSortering: Retning
  fnrSortering: Retning
}

export const initialSortering: OppgaveSortering = {
  registreringsdatoSortering: 'none',
  fristSortering: 'none',
  fnrSortering: 'none',
}

type Retning = 'ascending' | 'descending' | 'none'

const sammenlignDato = (a: OppgaveDTO | GosysOppgave, b: OppgaveDTO | GosysOppgave) => {
  // Konverterer datoene til en numerisk verdi og sammenligner dem
  return (!!a.frist ? new Date(a.frist).getTime() : 0) - (!!b.frist ? new Date(b.frist).getTime() : 0)
}

function sorterDato(retning: Retning, oppgaver: OppgaveDTO[] | GosysOppgave[]) {
  switch (retning) {
    case 'ascending':
      return oppgaver.sort(sammenlignDato)
    case 'descending':
      return oppgaver.sort(sammenlignDato).reverse()
    case 'none':
      return oppgaver
  }
}

const sammenlignFnr = (a: OppgaveDTO, b: OppgaveDTO) => {
  // Sammenligner de første 6 sifrene i fødselsnummerene
  return (!!a.fnr ? Number(a.fnr.slice(0, 5)) : 0) - (!!b.fnr ? Number(b.fnr.slice(0, 5)) : 0)
}

function sorterFnr(retning: Retning, oppgaver: OppgaveDTO[]) {
  switch (retning) {
    case 'ascending':
      return oppgaver.sort(sammenlignFnr)
    case 'descending':
      return oppgaver.sort(sammenlignFnr).reverse()
    case 'none':
      return oppgaver
  }
}

const sammenlignGosysBruker = (a: GosysOppgave, b: GosysOppgave) =>
  (!!a.bruker?.ident ? Number(a.bruker?.ident.slice(0, 5)) : 0) -
  (!!b.bruker?.ident ? Number(b.bruker?.ident.slice(0, 5)) : 0)

const sorterGosysBruker = (retning: Retning, oppgaver: GosysOppgave[]) => {
  switch (retning) {
    case 'ascending':
      return oppgaver.sort(sammenlignGosysBruker)
    case 'descending':
      return oppgaver.sort(sammenlignGosysBruker).reverse()
    case 'none':
      return oppgaver
  }
}

export function sorterOppgaver(oppgaver: OppgaveDTO[], sortering: OppgaveSortering): OppgaveDTO[] {
  const sortertRegistreringsdato = sorterDato(sortering.registreringsdatoSortering, oppgaver)
  const sortertFrist = sorterDato(sortering.fristSortering, sortertRegistreringsdato) as OppgaveDTO[]

  return sorterFnr(sortering.fnrSortering, sortertFrist)
}

export function sorterGosysOppgaver(oppgaver: GosysOppgave[], sortering: OppgaveSortering): GosysOppgave[] {
  const sortertRegistreringsdato = sorterDato(sortering.registreringsdatoSortering, oppgaver)
  const sortertFrist = sorterDato(sortering.fristSortering, sortertRegistreringsdato) as GosysOppgave[]

  return sorterGosysBruker(sortering.fnrSortering, sortertFrist)
}

const SORTERING_KEY_LOCAL_STORAGE = 'OPPGAVESORTERING'

export const leggTilSorteringILocalStorage = (oppgaveSortering: OppgaveSortering) =>
  localStorage.setItem(SORTERING_KEY_LOCAL_STORAGE, JSON.stringify(oppgaveSortering))

export const hentSorteringFraLocalStorage = (): OppgaveSortering => {
  try {
    const sorteringFraLocalStorage = localStorage[SORTERING_KEY_LOCAL_STORAGE]
    if (!!sorteringFraLocalStorage) {
      return JSON.parse(sorteringFraLocalStorage)
    } else {
      return initialSortering
    }
  } catch {
    logger.generalError({ msg: 'Feil i hentingen av sortering fra localstorage' })
    return initialSortering
  }
}
