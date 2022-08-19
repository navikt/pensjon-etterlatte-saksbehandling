import { IApiResponse } from './types'

const path = process.env.REACT_APP_VEDTAK_URL

export const getPerson = async (fnr: string): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/personer/${fnr}`)
    const data = await result.json()
    if (result.ok) {
      return { status: result.status, data: data }
    } else {
      console.log('error i fetch', result)
      if (data.includes('Ugyldig fødselsnummer')) {
        return { status: 500, data: data }
      } else {
        return { status: result.status, data: 'Det skjedde en feil' }
      }
    }
  } catch (e) {
    console.log('error', e)
    return { status: 500 }
  }
}
