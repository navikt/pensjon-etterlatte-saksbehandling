import { IApiResponse } from './types'

const path = process.env.REACT_APP_VEDTAK_URL

export const getPerson = async (fnr: string): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/personer/${fnr}`)
    return {
      status: result.status,
      data: await result.json(),
    }
  } catch (e) {
    return { status: 500 }
  }
}
