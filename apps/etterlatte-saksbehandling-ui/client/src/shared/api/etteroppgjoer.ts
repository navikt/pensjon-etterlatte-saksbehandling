import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  EtteroppgjoerForbehandling,
  EtteroppgjoerBehandling,
  FaktiskInntekt,
  BeregnetEtteroppgjoerResultatDto,
  Etteroppgjoer,
} from '~shared/types/EtteroppgjoerForbehandling'
import { OppgaveDTO } from '~shared/types/oppgave'

interface EtteroppgjoerOgOppgave {
  etteroppgjoerBehandling: EtteroppgjoerForbehandling
  oppgave: OppgaveDTO
}

export const hentEtteroppgjoer = async (sakId: string): Promise<ApiResponse<Etteroppgjoer[]>> => {
  return apiClient.get(`/etteroppgjoer/${sakId}`)
}

export const hentEtteroppgjoerForbehandling = async (
  behandlingId: string
): Promise<ApiResponse<EtteroppgjoerForbehandling>> => {
  return apiClient.get(`/etteroppgjoer/forbehandling/${behandlingId}`)
}

export const opprettEtteroppgjoerIDev = async (sakId: number): Promise<ApiResponse<EtteroppgjoerOgOppgave>> => {
  return apiClient.post(`/etteroppgjoer/kundev/${sakId}`, {})
}

export const hentEtteroppgjoerForbehandlinger = async (
  sakId: number
): Promise<ApiResponse<EtteroppgjoerBehandling[]>> => {
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

export const lagreHarMottattNyInformasjon = async (args: {
  forbehandlingId: string
  harMottattNyInformasjon: boolean
}) => {
  return apiClient.post(`/etteroppgjoer/forbehandling/${args.forbehandlingId}/har-mottatt-ny-informasjon`, {
    harMottattNyInformasjon: args.harMottattNyInformasjon,
  })
}

export const lagreEndringErTilUgunstForBruker = async (args: {
  forbehandlingId: string
  endringErTilUgunstForBruker: boolean
  beskrivelseAvUgunst: string
}) => {
  return apiClient.post(`/etteroppgjoer/forbehandling/${args.forbehandlingId}/endring-er-til-ugunst-for-bruker`, {
    endringErTilUgunstForBruker: args.endringErTilUgunstForBruker,
    beskrivelseAvUgunst: args.beskrivelseAvUgunst,
  })
}

export const ferdigstillEtteroppgjoerForbehandlingBrev = async (args: {
  forbehandlingId: string
}): Promise<ApiResponse<any>> => {
  return apiClient.post(`etteroppgjoer/forbehandling/${args.forbehandlingId}/ferdigstill`, {})
}
