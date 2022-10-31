import { IApiResponse } from './types'
import { IDetaljertBehandling, Virkningstidspunkt } from '../../store/reducers/BehandlingReducer'
import { apiClient, ApiResponse } from './apiClient'

const path = process.env.REACT_APP_VEDTAK_URL

export const hentBehandling = async (id: string): Promise<ApiResponse<IDetaljertBehandling>> => {
  return apiClient.get(`/behandling/${id}`)
}

export const avbrytBehandling = async (behandlingsid: string): Promise<IApiResponse<any>> => {
  try {
    const result: Response = await fetch(`${path}/api/behandling/${behandlingsid}/avbryt`, {
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

export const fastsettVirkningstidspunkt = async (id: string, dato: Date): Promise<ApiResponse<Virkningstidspunkt>> => {
  return apiClient.post(`/behandling/${id}/virkningstidspunkt`, { dato })
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

export interface GrunnlagResponse {
  response: string
}

export const slettPeriodeForAvdoedesMedlemskap = async (
  behandlingsId: string,
  saksbehandlerPeriodeId: string
): Promise<ApiResponse<GrunnlagResponse>> =>
  apiClient.delete<GrunnlagResponse>(`/grunnlag/saksbehandler/periode/${behandlingsId}/${saksbehandlerPeriodeId}`)
