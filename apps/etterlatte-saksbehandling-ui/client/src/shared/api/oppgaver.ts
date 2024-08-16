import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { konverterOppgavestatusFilterValuesTilKeys } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { OppgavebenkStats } from '~components/oppgavebenk/state/oppgavebenkState'
import { GenerellEndringshendelse, GenerellOppgaveDto, NyOppgaveDto, OppgaveDTO } from '~shared/types/oppgave'

export const hentOppgaverMedStatus = async (args: {
  oppgavestatusFilter: Array<string>
  minOppgavelisteIdent?: boolean
}): Promise<ApiResponse<OppgaveDTO[]>> => {
  const konverterteFiltre = konverterOppgavestatusFilterValuesTilKeys(args.oppgavestatusFilter)

  const queryParams = konverterteFiltre
    .map((i) => `oppgaveStatus=${i}&`)
    .join('')
    .slice(0, -1)

  const identfilterGenerator = () => {
    if (args.minOppgavelisteIdent) {
      return `&kunInnloggetOppgaver=${args.minOppgavelisteIdent}`
    }
    return ''
  }
  return apiClient.get(`/oppgaver?${queryParams}${identfilterGenerator()}`)
}

export const hentOppgave = async (id: string): Promise<ApiResponse<OppgaveDTO>> => apiClient.get(`/oppgaver/${id}`)

export const hentEndringer = async (id: string): Promise<ApiResponse<GenerellEndringshendelse[]>> =>
  apiClient.get(`/oppgaver/${id}/endringer`)

export const hentOppgaverMedReferanse = async (referanse: string): Promise<ApiResponse<OppgaveDTO[]>> =>
  apiClient.get(`/oppgaver/referanse/${referanse}`)

export const hentOppgaveForReferanseUnderBehandling = async (referanse: string): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.get(`/oppgaver/referanse/${referanse}/underbehandling`)

export const hentOppgavebenkStats = async (): Promise<ApiResponse<OppgavebenkStats>> => apiClient.get('/oppgaver/stats')

export const hentOppgaverTilknyttetSak = async (sakId: number): Promise<ApiResponse<Array<OppgaveDTO>>> => {
  return apiClient.get(`/oppgaver/sak/${sakId}/oppgaver`)
}

export const opprettGenerellOppgave = async (generellOppgave: GenerellOppgaveDto): Promise<ApiResponse<any>> =>
  apiClient.post('/oppgaver/bulk/opprett', { ...generellOppgave })

export const opprettOppgave = async (args: {
  sakId: number
  request: NyOppgaveDto
}): Promise<ApiResponse<OppgaveDTO>> => apiClient.post(`/oppgaver/sak/${args.sakId}/opprett`, { ...args.request })

export const ferdigstillOppgave = async (id: string): Promise<ApiResponse<any>> => ferdigstillOppgaveMedMerknad({ id })

export const ferdigstillOppgaveMedMerknad = async (args: {
  id: string
  merknad?: string | null
}): Promise<ApiResponse<any>> => apiClient.put(`/oppgaver/${args.id}/ferdigstill`, { merknad: args.merknad })

export interface OppdatertOppgaveversjonResponseDto {
  versjon: number | null
}

export interface SaksbehandlerEndringDto {
  saksbehandler: string
  versjon: number | null
}

export const saksbehandlereIEnhetApi = async (args: {
  enheter: string[]
}): Promise<ApiResponse<Array<Saksbehandler>>> => {
  return apiClient.get(`/saksbehandlere?enheter=${args.enheter}`)
}

export const tildelSaksbehandlerApi = async (args: {
  oppgaveId: string
  nysaksbehandler: SaksbehandlerEndringDto
}): Promise<ApiResponse<OppdatertOppgaveversjonResponseDto>> => {
  return apiClient.post(`/oppgaver/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
}

export const fjernSaksbehandlerApi = async (args: {
  oppgaveId: string
  sakId: number
}): Promise<ApiResponse<OppdatertOppgaveversjonResponseDto>> => {
  return apiClient.delete(`/oppgaver/${args.oppgaveId}/saksbehandler`)
}

export interface RedigerFristRequest {
  frist: Date
  versjon: number | null
}

export const redigerFristApi = async (args: {
  oppgaveId: string
  redigerFristRequest: RedigerFristRequest
}): Promise<ApiResponse<void>> => {
  return apiClient.put(`/oppgaver/${args.oppgaveId}/frist`, { ...args.redigerFristRequest })
}

export interface EndrePaaVentRequest {
  aarsak?: string
  merknad: String
  paaVent: boolean
}

export const settOppgavePaaVentApi = async (args: {
  oppgaveId: string
  settPaaVentRequest: EndrePaaVentRequest
}): Promise<ApiResponse<OppgaveDTO>> => {
  return apiClient.post(`/oppgaver/${args.oppgaveId}/sett-paa-vent`, { ...args.settPaaVentRequest })
}
