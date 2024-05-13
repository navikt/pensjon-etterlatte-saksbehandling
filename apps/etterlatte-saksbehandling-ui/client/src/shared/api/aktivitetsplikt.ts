import { apiClient, ApiResponse } from '~shared/api/apiClient'
import {
  AktivitetspliktOppfolging,
  IAktivitet,
  IAktivitetspliktVurdering,
  IOpprettAktivitet,
  IOpprettAktivitetspliktVurdering,
} from '~shared/types/Aktivitetsplikt'

export const hentAktivitetspliktOppfolging = async (args: {
  behandlingId: string
}): Promise<ApiResponse<AktivitetspliktOppfolging>> => apiClient.get(`/behandling/${args.behandlingId}/aktivitetsplikt`)

export interface OpprettAktivitetspliktOppfolgingRequest {
  aktivitet: string
}

export const opprettAktivitetspliktOppfolging = async (args: {
  behandlingId: string
  request: OpprettAktivitetspliktOppfolgingRequest
}): Promise<ApiResponse<AktivitetspliktOppfolging>> =>
  apiClient.post(`/behandling/${args.behandlingId}/aktivitetsplikt`, { ...args.request })

export const hentAktiviteter = async (args: { behandlingId: string }): Promise<ApiResponse<IAktivitet[]>> =>
  apiClient.get(`/behandling/${args.behandlingId}/aktivitetsplikt/aktivitet`)

export const opprettAktivitet = async (args: {
  behandlingId: string
  request: IOpprettAktivitet
}): Promise<ApiResponse<IAktivitet[]>> =>
  apiClient.post(`/behandling/${args.behandlingId}/aktivitetsplikt/aktivitet`, { ...args.request })

export const slettAktivitet = async (args: {
  behandlingId: string
  aktivitetId: string
}): Promise<ApiResponse<IAktivitet[]>> =>
  apiClient.delete(`/behandling/${args.behandlingId}/aktivitetsplikt/aktivitet/${args.aktivitetId}`)

export const hentAktivitspliktVurdering = async (args: {
  sakId: number
  oppgaveId: string
}): Promise<ApiResponse<IAktivitetspliktVurdering>> =>
  apiClient.get(`/sak/${args.sakId}/oppgave/${args.oppgaveId}/aktivitetsplikt/vurdering`)

export const opprettAktivitspliktVurdering = async (args: {
  sakId: number
  oppgaveId: string
  request: IOpprettAktivitetspliktVurdering
}): Promise<ApiResponse<IAktivitetspliktVurdering>> =>
  apiClient.post(`/sak/${args.sakId}/oppgave/${args.oppgaveId}/aktivitetsplikt/vurdering`, { ...args.request })
