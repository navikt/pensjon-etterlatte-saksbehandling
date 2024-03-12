import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { SakType } from '~shared/types/sak'
import { konverterOppgavestatusFilterValuesTilKeys } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { OppgavebenkStats } from '~components/oppgavebenk/utils/oppgavebenkStats'

export interface OppgaveDTO {
  id: string
  status: Oppgavestatus
  enhet: string
  sakId: number
  type: Oppgavetype
  kilde: OppgaveKilde
  referanse: string | null
  merknad?: string
  opprettet: string
  sakType: SakType
  fnr: string | null
  frist: string
  saksbehandler: OppgaveSaksbehandler | null

  // GOSYS-spesifikt
  beskrivelse: string | null
  journalpostId: string | null
  gjelder: string | null
  versjon: number | null
}

export interface OppgaveSaksbehandler {
  ident: string
  navn?: string
}

export interface NyOppgaveDto {
  oppgaveKilde?: OppgaveKilde
  oppgaveType: Oppgavetype
  merknad?: string
  referanse?: string
}

export type Oppgavestatus = 'NY' | 'UNDER_BEHANDLING' | 'PAA_VENT' | 'FERDIGSTILT' | 'FEILREGISTRERT' | 'AVBRUTT'
export type OppgaveKilde =
  | 'HENDELSE'
  | 'BEHANDLING'
  | 'EKSTERN'
  | 'GENERELL_BEHANDLING'
  | 'TILBAKEKREVING'
  | 'SAKSBEHANDLER'

export type Oppgavetype =
  | 'FOERSTEGANGSBEHANDLING'
  | 'REVURDERING'
  | 'VURDER_KONSEKVENS'
  | 'ATTESTERING'
  | 'UNDERKJENT'
  | 'GOSYS'
  | 'KRAVPAKKE_UTLAND'
  | 'KLAGE'
  | 'TILBAKEKREVING'
  | 'OMGJOERING'
  | 'JOURNALFOERING'
  | 'GJENOPPRETTING_ALDERSOVERGANG'

export const erOppgaveRedigerbar = (status: Oppgavestatus): boolean =>
  ['NY', 'UNDER_BEHANDLING', 'PAA_VENT'].includes(status)

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
export const hentGosysOppgaver = async (): Promise<ApiResponse<OppgaveDTO[]>> => apiClient.get('/oppgaver/gosys')

export const hentOppgavebenkStats = async (): Promise<ApiResponse<OppgavebenkStats>> => apiClient.get('/oppgaver/stats')

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
  versjon: number
}): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.post(`/oppgaver/gosys/${args.oppgaveId}/feilregistrer?versjon=${args.versjon}`, {})

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

export const saksbehandlereIEnhetApi = async (args: {
  enheter: string[]
}): Promise<ApiResponse<Array<Saksbehandler>>> => {
  return apiClient.get(`/saksbehandlere?enheter=${args.enheter}`)
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

export interface EndrePaaVentRequest {
  merknad: String
  paaVent: boolean
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

export const settOppgavePaaVentApi = async (args: {
  oppgaveId: string
  settPaaVentRequest: EndrePaaVentRequest
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`/oppgaver/${args.oppgaveId}/sett-paa-vent`, { ...args.settPaaVentRequest })
}

export const hentOppgaveForBehandlingUnderBehandlingIkkeattestert = async (args: {
  referanse: string
  sakId: number
}): Promise<ApiResponse<Saksbehandler>> => apiClient.get(`/oppgaver/sak/${args.sakId}/ikkeattestert/${args.referanse}`)

export const hentOppgaveForBehandlingUnderBehandlingIkkeattestertOppgave = async (args: {
  referanse: string
  sakId: number
}): Promise<ApiResponse<OppgaveDTO>> =>
  apiClient.get(`/oppgaver/sak/${args.sakId}/ikkeattestertOppgave/${args.referanse}`)

export const hentSaksbehandlerForReferanseOppgaveUnderArbeid = async (args: {
  referanse: string
  sakId: number
}): Promise<ApiResponse<Saksbehandler>> =>
  apiClient.get(`/oppgaver/sak/${args.sakId}/oppgaveunderbehandling/${args.referanse}`)
