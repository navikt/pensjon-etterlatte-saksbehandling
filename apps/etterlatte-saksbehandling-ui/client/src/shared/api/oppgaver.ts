import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { konverterOppgavestatusFilterValuesTilKeys } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { OppgavebenkStats } from '~components/oppgavebenk/state/oppgavebenkState'
import { NyOppgaveDto, OppgaveDTO, Oppgavetype } from '~shared/types/oppgave'
import { GosysTema } from '~shared/types/Gosys'

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

export const hentOppgaverMedReferanse = async (referanse: string): Promise<ApiResponse<OppgaveDTO[]>> =>
  apiClient.get(`/oppgaver/referanse/${referanse}`)

export const hentOppgaveForReferanseUnderBehandling = async (referanse: string): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.get(`/oppgaver/referanse/${referanse}/underbehandling`)

export const hentSaksbehandlerForOppgaveUnderBehandling = async (
  referanse: string
): Promise<ApiResponse<Saksbehandler>> =>
  apiClient.get(`/oppgaver/referanse/${referanse}/saksbehandler-underbehandling`)

export const hentGosysOppgaver = async (tema: GosysTema[]): Promise<ApiResponse<OppgaveDTO[]>> => {
  const queryParams = tema.map((t) => `tema=${t}`).join('&')

  return apiClient.get(`/oppgaver/gosys?${queryParams}`)
}

export const hentOppgavebenkStats = async (): Promise<ApiResponse<OppgavebenkStats>> => apiClient.get('/oppgaver/stats')

export const hentOppgaverTilknyttetSak = async (sakId: number): Promise<ApiResponse<Array<OppgaveDTO>>> => {
  return apiClient.get(`/oppgaver/sak/${sakId}/oppgaver`)
}

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

export const ferdigstilleGosysOppgave = async (args: {
  oppgaveId: string
  versjon: number
}): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/ferdigstill?versjon=${args.versjon}`, {})

export const feilregistrerGosysOppgave = async (args: {
  oppgaveId: string
  beskrivelse: string
  versjon: number
}): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/feilregistrer`, {
    versjon: args.versjon,
    beskrivelse: args.beskrivelse,
  })

export const saksbehandlereIEnhetApi = async (args: {
  enheter: string[]
}): Promise<ApiResponse<Array<Saksbehandler>>> => {
  return apiClient.get(`/saksbehandlere?enheter=${args.enheter}`)
}

export const byttSaksbehandlerApi = async (args: {
  oppgaveId: string
  type: Oppgavetype
  nysaksbehandler: SaksbehandlerEndringDto
}): Promise<ApiResponse<OppdatertOppgaveversjonResponseDto>> => {
  if (args.type == Oppgavetype.GOSYS) {
    return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/tildel-saksbehandler`, { ...args.nysaksbehandler })
  } else {
    return apiClient.post(`/oppgaver/${args.oppgaveId}/bytt-saksbehandler`, { ...args.nysaksbehandler })
  }
}

export const fjernSaksbehandlerApi = async (args: {
  oppgaveId: string
  sakId: number
  type: Oppgavetype
  versjon: number | null
}): Promise<ApiResponse<OppdatertOppgaveversjonResponseDto>> => {
  if (args.type == Oppgavetype.GOSYS) {
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

export interface EndrePaaVentRequest {
  merknad: String
  paaVent: boolean
}

export const redigerFristApi = async (args: {
  oppgaveId: string
  type: Oppgavetype
  redigerFristRequest: RedigerFristRequest
}): Promise<ApiResponse<void>> => {
  if (args.type == Oppgavetype.GOSYS) {
    return apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/endre-frist`, { ...args.redigerFristRequest })
  } else {
    return apiClient.put(`/oppgaver/${args.oppgaveId}/frist`, { ...args.redigerFristRequest })
  }
}

export const settOppgavePaaVentApi = async (args: {
  oppgaveId: string
  settPaaVentRequest: EndrePaaVentRequest
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/oppgaver/${args.oppgaveId}/sett-paa-vent`, { ...args.settPaaVentRequest })
}
