import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { ISak, SakType } from '~shared/types/sak'
import { SakMedBehandlinger } from '~components/person/typer'

export interface Navkontor {
  navn: string
}

export interface Flyktning {
  erFlyktning: boolean
  virkningstidspunkt: string
}

export const hentNavkontorForPerson = async (fnr: string): Promise<ApiResponse<Navkontor>> => {
  return apiClient.post(`/personer/navkontor`, { foedselsnummer: fnr })
}

export const hentSakMedBehandlnger = async (fnr: string): Promise<ApiResponse<SakMedBehandlinger>> => {
  return apiClient.post(`/personer/behandlingerforsak`, { foedselsnummer: fnr })
}

interface SisteIverksatteBehandling {
  id: string
}

export const hentSisteIverksatteBehandlingId = async (
  sakId: number
): Promise<ApiResponse<SisteIverksatteBehandling>> => {
  return apiClient.get(`/sak/${sakId}/behandlinger/sisteIverksatte`)
}

export const hentSak = async (sakId: number): Promise<ApiResponse<ISak>> => {
  return apiClient.get(`sak/${sakId}`)
}

export const hentFlyktningStatusForSak = async (sakId: number): Promise<ApiResponse<Flyktning>> => {
  return apiClient.get(`sak/${sakId}/flyktning`)
}

export const hentSakForPerson = async (args: {
  fnr: string
  type: SakType
  opprettHvisIkkeFinnes?: boolean
}): Promise<ApiResponse<ISak>> => {
  if (args.opprettHvisIkkeFinnes) {
    return apiClient.post(`/personer/sak/${args.type}?opprettHvisIkkeFinnes=true`, { foedselsnummer: args.fnr })
  } else {
    return apiClient.post(`/personer/sak/${args.type}`, { foedselsnummer: args.fnr })
  }
}

export const byttEnhetPaaSak = async (args: { sakId: number; enhet: string }): Promise<ApiResponse<void>> => {
  return apiClient.post(`sak/${args.sakId}/endre-enhet`, { enhet: args.enhet })
}

export const oppdaterIdentPaaSak = async (args: {
  sakId: number
  hendelseId?: string
  utenHendelse: boolean
}): Promise<ApiResponse<ISak>> => {
  return apiClient.post(`sak/${args.sakId}/oppdater-ident`, {
    hendelseId: args.hendelseId,
    utenHendelse: args.utenHendelse,
  })
}
