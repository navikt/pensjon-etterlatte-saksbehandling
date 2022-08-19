import { IApiResponse } from './types'

const path = process.env.REACT_APP_VEDTAK_URL

export const getPerson = async (fnr: string): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/personer/${fnr}`)
    if (result.ok) {
      return { status: result.status, data: await result.json() }
    } else {
      console.log('error i fetch', result)
      return { status: 400, data: await result.json() }
    }
  } catch (e) {
    console.log('response errorrrrrr', e)
    return { status: 500, data: e }
  }
}
