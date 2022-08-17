import { IApiResponse } from './types'
import { IDetaljertBehandling } from "../../store/reducers/BehandlingReducer";

const path = process.env.REACT_APP_VEDTAK_URL

export const hentBehandling = async (id: string): Promise<IApiResponse<IDetaljertBehandling>> => {
  try {
    const result: Response = await fetch(`${path}/api/behandling/${id}`)
    const data: IDetaljertBehandling = await result.json()

    return {
      status: result.status,
      data: data,
    }
  } catch (e) {
    return {status: 500 }
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
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}

export const underkjennVedtak = async (
  behandlingId: string,
  kommentar: string,
  valgtBegrunnelse: string
): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/underkjennvedtak/${behandlingId}`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        kommentar: kommentar,
        valgtBegrunnelse: valgtBegrunnelse,
      }),
    })
    return {
      status: result.status,
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}

export const lagreBegrunnelseKommerBarnetTilgode = async (
  behandlingsId: string,
  kommentar: string,
  svar: string
): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/grunnlag/kommertilgode/${behandlingsId}`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        svar: svar,
        begrunnelse: kommentar,
      }),
    })
    return {
      status: result.status,
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}

export const lagreSoeskenMedIBeregning = async (
  behandlingsId: string,
  soeskenMedIBeregning: { foedselsnummer: string; skalBrukes: boolean }[]
): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/grunnlag/beregningsgrunnlag/${behandlingsId}`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(soeskenMedIBeregning),
    })
    return {
      status: result.status,
    }
  } catch (e) {
    console.log(e)
    return { status: 500 }
  }
}
