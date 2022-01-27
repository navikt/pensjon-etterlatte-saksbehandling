import { mockdata } from '../../components/oppgavebenken/OppgaveKolonner'
import { IApiResponse } from './types'
import { IOppgave } from '../../components/oppgavebenken/typer/oppgavebenken'

const isDev = process.env.NODE_ENV !== 'production'
const path = isDev ? 'http://localhost:8080' : 'https://etterlatte-saksbehandling.dev.intern.nav.no'

export const hentMockOppgaver = async () => {
  try {
    const response: any = await mockdata
    return response
  } catch (e: any) {
    throw new Error('Det skjedde en feil')
  }
}

//ikke sikkert vi trenger denne etter mapping til oppgaver, lar stå enn så lenge
export const hentSaker = async (): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/saker`)
    return {
      status: result.status,
      data: await result.json(),
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}

export const hentOppgaver = async (): Promise<IApiResponse<ReadonlyArray<IOppgave>>> => {
  try {
    const result: Response = await fetch(`${path}/api/oppgaver`)
    return {
      status: result.status,
      data: await result.json(),
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}
