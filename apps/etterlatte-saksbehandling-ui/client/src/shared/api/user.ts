import { IApiResponse } from './types'

const path = process.env.REACT_APP_VEDTAK_URL;

console.log('path', path)

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
