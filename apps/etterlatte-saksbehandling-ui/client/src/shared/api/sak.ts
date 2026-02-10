import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { ISak, ISaksendring, SakType } from '~shared/types/sak'
import { SakMedBehandlingerOgKanskjeAnnenSak } from '~components/person/typer'
import { Personopplysninger } from '~shared/types/grunnlag'
import { hentPersonopplysningerForBehandling } from '~shared/api/grunnlag'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

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

export const hentSakMedBehandlnger = async (fnr: string): Promise<ApiResponse<SakMedBehandlingerOgKanskjeAnnenSak>> => {
  return apiClient.post(`/personer/behandlingerforsak`, { foedselsnummer: fnr })
}

export function hentBehandlingerISak(args: { sakId: number }): Promise<ApiResponse<IDetaljertBehandling[]>> {
  return apiClient.get(`/sak/${args.sakId}/behandlinger`)
}

interface SisteIverksatteBehandling {
  id: string
}

export const hentSisteIverksatteBehandlingId = async (
  sakId: number
): Promise<ApiResponse<SisteIverksatteBehandling>> => {
  return apiClient.get(`/sak/${sakId}/behandlinger/sisteIverksatte`)
}

export const hentSisteIverksattePersonopplysninger = async (args: {
  sakId: number
  sakType: SakType
}): Promise<ApiResponse<Personopplysninger>> => {
  const sistIverksatteBehandling = await hentSisteIverksatteBehandlingId(args.sakId)
  if (sistIverksatteBehandling.ok) {
    return await hentPersonopplysningerForBehandling({
      behandlingId: sistIverksatteBehandling.data.id,
      sakType: args.sakType,
    })
  } else {
    // Vi har en feil som vi kan propagere videre
    return sistIverksatteBehandling
  }
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

export const byttEnhetPaaSak = async (args: {
  sakId: number
  enhet: string
  kommentar?: string
}): Promise<ApiResponse<void>> => {
  return apiClient.post(`sak/${args.sakId}/endre-enhet`, { enhet: args.enhet, kommentar: args.kommentar })
}

export const hentSaksendringer = async (sakId: number): Promise<ApiResponse<ISaksendring[]>> => {
  return apiClient.get(`sak/${sakId}/endringer`)
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
