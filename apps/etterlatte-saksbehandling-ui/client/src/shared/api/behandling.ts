import { IApiResponse } from './types'

const path = process.env.REACT_APP_VEDTAK_URL

export const hentBehandling = async (id: string): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/behandling/${id}`)
    const data: any = await result.json()

    return {
      status: result.status,
      data: data,
    }
  } catch (e) {
    return { status: 500 }
  }
}

export const avbrytBehandling = async (behandlingsid: string): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/avbrytBehandling/${behandlingsid}`, {
      method: 'post',
    })
    return {
      status: result.status,
      data: await result.json(),
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}

export const fattVedtak = async (behandlingsId: string): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/fattvedtak/${behandlingsId}`, {
      method: 'post',
    })
    return {
      status: result.status,
      data: await result.json(),
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}

export const attesterVedtak = async (behandlingId: string): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/attestervedtak/${behandlingId}`, {
      method: 'post',
    })
    return {
      status: result.status,
      data: await result.json(),
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}

export const underkjennVedtak = async (behandlingId: string): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/underkjennvedtak/${behandlingId}`, {
      method: 'post',
    })
    return {
      status: result.status,
      data: await result.json(),
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}
