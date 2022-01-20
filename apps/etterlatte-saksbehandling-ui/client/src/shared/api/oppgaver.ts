import { mockdata } from '../../components/oppgavebenken/OppgaveKolonner'
import { IApiResponse } from './types'

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

export const hentOppgaver = async (): Promise<IApiResponse<any>> => {
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
