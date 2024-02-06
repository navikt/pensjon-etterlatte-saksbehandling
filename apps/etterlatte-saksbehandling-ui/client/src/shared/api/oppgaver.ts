import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { SakType } from '~shared/types/sak'

export interface OppgaveDTO {
  id: string
  status: Oppgavestatus
  enhet: string
  sakId: number
  type: Oppgavetype
  kilde: OppgaveKilde
  saksbehandler: string | null
  referanse: string | null
  merknad: string | null
  opprettet: string
  sakType: SakType
  fnr: string
  frist: string
  //Oppgaveliste spesifikt
  saksbehandlerNavn: string | null

  // GOSYS-spesifikt
  beskrivelse: string | null
  gjelder: string | null
  versjon: number | null
}

export interface NyOppgaveDto {
  oppgaveKilde?: OppgaveKilde
  oppgaveType: Oppgavetype
  merknad?: string
  referanse?: string
}

export type Oppgavestatus = 'NY' | 'UNDER_BEHANDLING' | 'FERDIGSTILT' | 'FEILREGISTRERT' | 'AVBRUTT'
export type OppgaveKilde = 'HENDELSE' | 'BEHANDLING' | 'EKSTERN' | 'GENERELL_BEHANDLING' | 'TILBAKEKREVING'
export type Oppgavetype =
  | 'FOERSTEGANGSBEHANDLING'
  | 'REVURDERING'
  | 'MANUELT_OPPHOER'
  | 'VURDER_KONSEKVENS'
  | 'ATTESTERING'
  | 'UNDERKJENT'
  | 'GOSYS'
  | 'KRAVPAKKE_UTLAND'
  | 'KLAGE'
  | 'TILBAKEKREVING'
  | 'OMGJOERING'
  | 'JOURNALFOERING'

export const erOppgaveRedigerbar = (status: Oppgavestatus): boolean => ['NY', 'UNDER_BEHANDLING'].includes(status)

export const hentOppgaver = async (): Promise<ApiResponse<OppgaveDTO[]>> => apiClient.get('/oppgaver')
export const hentOppgave = async (id: string): Promise<ApiResponse<OppgaveDTO>> => apiClient.get(`/oppgaver/${id}`)
export const hentGosysOppgaver = async (): Promise<ApiResponse<OppgaveDTO[]>> => apiClient.get('/oppgaver/gosys')

export const opprettOppgave = async (args: {
  sakId: number
  request: NyOppgaveDto
}): Promise<ApiResponse<OppgaveDTO>> => apiClient.post(`/oppgaver/sak/${args.sakId}/opprett`, { ...args.request })

export const ferdigstillOppgave = async (id: string): Promise<ApiResponse<any>> =>
  apiClient.put(`/oppgaver/${id}/ferdigstill`, {})

export interface OppdatertOppgaveversjonResponseDto {
  versjon: number | null
}

export interface SaksbehandlerEndringDto {
  saksbehandler: string
  versjon: number | null
}

export const tildelSaksbehandlerApi = async (args: {
  oppgaveId: string
  type: string
  nysaksbehandler: SaksbehandlerEndringDto
}): Promise<ApiResponse<OppdatertOppgaveversjonResponseDto>> => {
  if (args.type == 'GOSYS') {
    return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
  } else {
    return apiClient.post(`/oppgaver/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
  }
}

export const byttSaksbehandlerApi = async (args: {
  oppgaveId: string
  type: string
  nysaksbehandler: SaksbehandlerEndringDto
}): Promise<ApiResponse<OppdatertOppgaveversjonResponseDto>> => {
  if (args.type == 'GOSYS') {
    return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
  } else {
    return apiClient.post(`/oppgaver/${args.oppgaveId}/bytt-saksbehandler`, { ...args.nysaksbehandler })
  }
}

export const fjernSaksbehandlerApi = async (args: {
  oppgaveId: string
  sakId: number
  type: string
  versjon: number | null
}): Promise<ApiResponse<OppdatertOppgaveversjonResponseDto>> => {
  if (args.type == 'GOSYS') {
    return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, {
      saksbehandler: '',
      versjon: args.versjon,
    })
  } else {
    return apiClient.delete(`/oppgaver/${args.oppgaveId}/saksbehandler`)
  }
}

export interface RedigerFristRequest {
  frist: Date
  versjon: number | null
}
export const redigerFristApi = async (args: {
  oppgaveId: string
  type: string
  redigerFristRequest: RedigerFristRequest
}): Promise<ApiResponse<void>> => {
  if (args.type == 'GOSYS') {
    return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/endre-frist`, { ...args.redigerFristRequest })
  } else {
    return apiClient.put(`/oppgaver/${args.oppgaveId}/frist`, { ...args.redigerFristRequest })
  }
}

export const hentOppgaveForBehandlingUnderBehandlingIkkeattestert = async (args: {
  referanse: string
  sakId: number
}): Promise<ApiResponse<string>> => apiClient.get(`/oppgaver/sak/${args.sakId}/ikkeattestert/${args.referanse}`)

export const hentSaksbehandlerForReferanseOppgaveUnderArbeid = async (args: {
  referanse: string
  sakId: number
}): Promise<ApiResponse<string | null>> =>
  apiClient.get(`/oppgaver/sak/${args.sakId}/oppgaveunderbehandling/${args.referanse}`)
