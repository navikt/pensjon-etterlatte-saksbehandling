import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  Etteroppgjoer,
  EtteroppgjoerBehandling,
  FaktiskInntekt,
  BeregnetEtteroppgjoerResultatDto,
  EtteroppgjoerForbehandlingStatus,
} from '~shared/types/Etteroppgjoer'
import { OppgaveDTO } from '~shared/types/oppgave'

interface EtteroppgjoerOgOppgave {
  etteroppgjoerBehandling: Etteroppgjoer
  oppgave: OppgaveDTO
}

export const hentEtteroppgjoer = async (behandlingId: string): Promise<ApiResponse<Etteroppgjoer>> => {
  return apiClient.get(`/etteroppgjoer/${behandlingId}`)
}

export const opprettEtteroppgjoerIDev = async (sakId: number): Promise<ApiResponse<EtteroppgjoerOgOppgave>> => {
  return apiClient.post(`/etteroppgjoer/kundev/${sakId}`, {})
}

export const hentEtteroppgjoerForbehandlinger = async (
  sakId: number
): Promise<ApiResponse<EtteroppgjoerBehandling[]>> => {
  return apiClient.get(`/etteroppgjoer/forbehandlinger/${sakId}`)
}

export const oppdaterStatusPaaEtteroppgjoerForbehandling = async (args: {
  forbehandlingId: string
  nyStatus: EtteroppgjoerForbehandlingStatus
}): Promise<ApiResponse<any>> => {
  return apiClient.put(`/etteroppgjoer/${args.forbehandlingId}/`, { nyStatus: args.nyStatus })
}

export const ferdigstillEtteroppgjoerForbehandlingVarselbrev = async (args: {
  forbehandlingId: string
}): Promise<ApiResponse<any>> => {
  return apiClient.post(`etteroppgjoer/${args.forbehandlingId}/ferdigstill-forbehandling-varselbrev`, {})
}

export const lagreFaktiskInntekt = async (args: {
  forbehandlingId: string
  faktiskInntekt: FaktiskInntekt
}): Promise<ApiResponse<BeregnetEtteroppgjoerResultatDto>> => {
  return apiClient.post(`/etteroppgjoer/${args.forbehandlingId}/beregn_faktisk_inntekt`, {
    ...args.faktiskInntekt,
  })
}
