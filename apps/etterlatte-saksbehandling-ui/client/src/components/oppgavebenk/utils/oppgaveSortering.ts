import { logger } from '~utils/logger'
import { OppgaveDTO } from '~shared/api/oppgaver'

export interface OppgaveSortering {
  registreringsdatoSortering: Retning
  fristSortering: Retning
  fnrSortering: Retning
}

export const initialSortering = (): OppgaveSortering => {
  return {
    registreringsdatoSortering: 'none',
    fnrSortering: 'none',
    fristSortering: 'none',
  }
}

type Retning = 'descending' | 'ascending' | 'none'

const sammenlignFrist = (a: OppgaveDTO, b: OppgaveDTO) => {
  // Konverterer datoene til en numerisk verdi og sammenligner dem
  return (!!a.frist ? new Date(a.frist).getTime() : 0) - (!!b.frist ? new Date(b.frist).getTime() : 0)
}

export function sorterFrist(retning: Retning, oppgaver: OppgaveDTO[]) {
  switch (retning) {
    case 'ascending':
      return oppgaver.sort(sammenlignFrist)
    case 'descending':
      return oppgaver.sort(sammenlignFrist).reverse()
    case 'none':
      return oppgaver
  }
}

const sammenlignFnr = (a: OppgaveDTO, b: OppgaveDTO) => {
  // Sammenligner de første 6 sifrene i fødselsnummerene
  return (a.fnr ? Number(a.fnr.slice(0, 5)) : 0) - (b.fnr ? Number(b.fnr.slice(0, 5)) : 0)
}

export function sorterFnr(retning: Retning, oppgaver: OppgaveDTO[]) {
  switch (retning) {
    case 'ascending':
      return oppgaver.sort(sammenlignFnr)
    case 'descending':
      return oppgaver.sort(sammenlignFnr).reverse()
    case 'none':
      return oppgaver
  }
}

const SORTERING_KEY_LOCAL_STORAGE = 'OPPGAVESORTERING'

export const leggTilSorteringILocalStorage = (oppgaveSortering: OppgaveSortering) =>
  localStorage.setItem(SORTERING_KEY_LOCAL_STORAGE, JSON.stringify(oppgaveSortering))

export const hentSorteringFraLocalStorage = (): OppgaveSortering => {
  try {
    const sorteringFraLocalStorage = localStorage[SORTERING_KEY_LOCAL_STORAGE]
    if (!!sorteringFraLocalStorage) {
      const parsetFilter = JSON.parse(sorteringFraLocalStorage)
      const harGammelVersjon = Object.values(parsetFilter).find((value) => value === 'ingen')
      const harGammelVersjonNummerTo = Object.values(parsetFilter).find((value) => value === 'no-order')
      const harGammelVersjonNummerTre = Object.keys(parsetFilter).find((value) => value === 'registreringsdato')
      if (harGammelVersjon || harGammelVersjonNummerTo || !harGammelVersjonNummerTre) {
        const initiellSortering = initialSortering()
        leggTilSorteringILocalStorage(initiellSortering)
        return initiellSortering
      } else {
        return parsetFilter
      }
    } else {
      return initialSortering()
    }
  } catch (e) {
    logger.generalError({ message: 'Feil i hentingen av sortering fra localstorage' })
    return initialSortering()
  }
}
