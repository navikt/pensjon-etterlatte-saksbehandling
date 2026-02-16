import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  AvbrytEtteroppgjoerForbehandlingRequest,
  BeregnetEtteroppgjoerResultatDto,
  Etteroppgjoer,
  EtteroppgjoerForbehandling,
  DetaljertEtteroppgjoerForbehandling,
  FaktiskInntekt,
  IInformasjonFraBruker,
} from '~shared/types/EtteroppgjoerForbehandling'
import { JaNei } from '~shared/types/ISvar'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const hentEtteroppgjoerListe = async (sakId: string): Promise<ApiResponse<Etteroppgjoer[]>> => {
  return apiClient.get(`/etteroppgjoer/${sakId}/liste`)
}

export const opprettEtteroppgjoerForbehandlingIDev = async (args: {
  sakId: number
  inntektsaar: string
}): Promise<ApiResponse<EtteroppgjoerForbehandling>> => {
  return apiClient.post(`/etteroppgjoer/${args.sakId}/kundev-opprett-forbehandling`, {
    inntektsaar: args.inntektsaar,
  })
}

export const tilbakestillEtteroppgjoerOgOpprettOppgave = async (args: {
  sakId: number
  inntektsaar: string
}): Promise<ApiResponse<EtteroppgjoerForbehandling>> => {
  return apiClient.post(`/etteroppgjoer/${args.sakId}/tilbakestill-og-opprett-forbehandlingsoppgave`, {
    inntektsaar: args.inntektsaar,
  })
}

export const opprettEtteroppgoerForbehandling = async (args: {
  sakId: number
  oppgaveId: string
  inntektsaar: string
}): Promise<ApiResponse<EtteroppgjoerForbehandling>> => {
  return apiClient.post(`/etteroppgjoer/${args.sakId}/opprett-forbehandling/${args.oppgaveId}`, {
    inntektsaar: args.inntektsaar,
  })
}

export const hentEtteroppgjoerForbehandling = async (
  forbehandlingId: string
): Promise<ApiResponse<DetaljertEtteroppgjoerForbehandling>> => {
  return apiClient.get(`/etteroppgjoer/forbehandling/${forbehandlingId}`)
}

export const hentEtteroppgjoerForbehandlinger = async (
  sakId: number
): Promise<ApiResponse<EtteroppgjoerForbehandling[]>> => {
  return apiClient.get(`/etteroppgjoer/forbehandlinger/${sakId}`)
}

export const omgjoerEtteroppgjoerRevurdering = async (args: {
  behandlingId: string
}): Promise<ApiResponse<IDetaljertBehandling>> => {
  return apiClient.post(`/etteroppgjoer/revurdering/${args.behandlingId}/omgjoer`, {})
}

export const avbrytEtteroppgjoerForbehandling = async (args: {
  id: string
  avbrytEtteroppgjoerForbehandlingRequest: AvbrytEtteroppgjoerForbehandlingRequest
}): Promise<ApiResponse<unknown>> => {
  return apiClient.post(`/etteroppgjoer/forbehandling/${args.id}/avbryt`, {
    ...args.avbrytEtteroppgjoerForbehandlingRequest,
  })
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
  opphoerSkyldesDoedsfallIEtteroppgjoersaar?: JaNei
}) => {
  return apiClient.post(`/etteroppgjoer/forbehandling/${args.forbehandlingId}/opphoer-skyldes-doedsfall`, {
    opphoerSkyldesDoedsfall: args.opphoerSkyldesDoedsfall,
    opphoerSkyldesDoedsfallIEtteroppgjoersaar: args.opphoerSkyldesDoedsfallIEtteroppgjoersaar,
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
