import { IApiResponse } from './types'

const path = process.env.REACT_APP_VEDTAK_URL

export const hentBehandling = async (id: string): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/behandling/${id}`)
    const data: any = await result.json()
    console.log(data)
    return {
      status: result.status,
      data: data.behandling,
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}
