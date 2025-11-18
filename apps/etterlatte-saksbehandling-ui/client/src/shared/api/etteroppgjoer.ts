import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  AvbrytEtteroppgjoerForbehandlingRequest,
  BeregnetEtteroppgjoerResultatDto,
  Etteroppgjoer,
  EtteroppgjoerForbehandling,
  DetaljertEtteroppgjoer,
  FaktiskInntekt,
  IInformasjonFraBruker,
} from '~shared/types/EtteroppgjoerForbehandling'
import { JaNei } from '~shared/types/ISvar'

export const hentEtteroppgjoer = async (sakId: string): Promise<ApiResponse<Etteroppgjoer>> => {
  return apiClient.get(`/etteroppgjoer/${sakId}`)
}

export const opprettEtteroppgjoerForbehandlingIDev = async (
  sakId: number
): Promise<ApiResponse<EtteroppgjoerForbehandling>> => {
  return apiClient.post(`/etteroppgjoer/${sakId}/kundev-opprett-forbehandling`, {})
}

export const opprettEtteroppgoerForbehandling = async (args: {
  sakId: number
  oppgaveId: string
}): Promise<ApiResponse<EtteroppgjoerForbehandling>> => {
  return apiClient.post(`/etteroppgjoer/${args.sakId}/forbehandling/${args.oppgaveId}`, {})
}

export const hentEtteroppgjoerForbehandling = async (
  behandlingId: string
): Promise<ApiResponse<DetaljertEtteroppgjoer>> => {
  return apiClient.get(`/etteroppgjoer/forbehandling/${behandlingId}`)
}

export const avbrytEtteroppgjoerForbehandling = async (args: {
  id: string
  avbrytEtteroppgjoerForbehandlingRequest: AvbrytEtteroppgjoerForbehandlingRequest
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/etteroppgjoer/forbehandling/${args.id}/avbryt`, {
    ...args.avbrytEtteroppgjoerForbehandlingRequest,
  })
}

export const hentEtteroppgjoerForbehandlinger = async (
  sakId: number
): Promise<ApiResponse<EtteroppgjoerForbehandling[]>> => {
  return apiClient.get(`/etteroppgjoer/forbehandlinger/${sakId}`)
}

export const lagreFaktiskInntekt = async (args: {
  forbehandlingId: string
  faktiskInntekt: FaktiskInntekt
}): Promise<ApiResponse<BeregnetEtteroppgjoerResultatDto>> => {
  return apiClient.post(`/etteroppgjoer/forbehandling/${args.forbehandlingId}/beregn-faktisk-inntekt`, {
    ...args.faktiskInntekt,
  })
}

export const lagreInformasjonFraBruker = async (args: {
  forbehandlingId: string
  endringFraBruker: IInformasjonFraBruker
}) => {
  return apiClient.post(`/etteroppgjoer/forbehandling/${args.forbehandlingId}/informasjon-fra-bruker`, {
    ...args.endringFraBruker,
  })
}

export const lagreOmOpphoerSkyldesDoedsfall = async (args: {
  forbehandlingId: string
  opphoerSkyldesDoedsfall: JaNei
}) => {
  return apiClient.post(`/etteroppgjoer/forbehandling/${args.forbehandlingId}/opphoer-skyldes-doedsfall`, {
    opphoerSkyldesDoedsfall: args.opphoerSkyldesDoedsfall,
  })
}

export const ferdigstillEtteroppgjoerForbehandlingMedBrev = async (args: {
  forbehandlingId: string
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/etteroppgjoer/forbehandling/${args.forbehandlingId}/ferdigstill`, {})
}

export const ferdigstillEtteroppgjoerForbehandlingUtenBrev = async (args: {
  forbehandlingId: string
}): Promise<ApiResponse<EtteroppgjoerForbehandling>> => {
  return apiClient.post(`/etteroppgjoer/forbehandling/${args.forbehandlingId}/ferdigstill-uten-brev`, {})
}
