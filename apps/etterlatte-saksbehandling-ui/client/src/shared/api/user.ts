import { IApiResponse } from './types'

const isDev = process.env.NODE_ENV !== 'production'
const path = isDev ? 'http://localhost:8080' : 'https://etterlatte-saksbehandling.dev.intern.nav.no'

export const login = async (): Promise<IApiResponse<any>> => {
  // Bare tester litt
  try {
    const result: Response = await fetch('https://etterlatte-overvaaking.dev.intern.nav.no/')
    return {
      status: result.status,
      data: await result.json(),
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}

export const hentInnloggetSaksbehandler = async (): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/modiacontextholder/api/decorator`)
    return {
      status: result.status,
      data: await result.json(),
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}
