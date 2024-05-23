import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Grunnlagsopplysning, PersongalleriSamsvar, Personopplysninger } from '~shared/types/grunnlag'
import { IPersonResult } from '~components/person/typer'
import { Foreldreansvar } from '~shared/types/Foreldreansvar'
import { KildePdl } from '~shared/types/kilde'
import { IPdlPerson, Persongalleri } from '~shared/types/Person'
import { Mottaker } from '~shared/types/Brev'
import { SakType } from '~shared/types/sak'

export const getPerson = async (fnr: string): Promise<ApiResponse<IPersonResult>> => {
  return apiClient.post(`/grunnlag/person/navn`, {
    foedselsnummer: fnr,
  })
}

export const getGrunnlagsAvOpplysningstype = async (args: {
  sakId: number
  behandlingId: string
  opplysningstype: string
}): Promise<ApiResponse<Grunnlagsopplysning<IPdlPerson, KildePdl>>> => {
  return apiClient.get(`/grunnlag/behandling/${args.behandlingId}/${args.opplysningstype}`)
}

export const getVergeadresseForPerson = async (foedselsnummer: string): Promise<ApiResponse<Mottaker>> => {
  return apiClient.post(`/grunnlag/person/vergeadresse`, {
    foedselsnummer: foedselsnummer,
  })
}

export const getHistoriskForeldreansvar = (args: {
  sakId: number
  behandlingId: string
}): Promise<ApiResponse<Grunnlagsopplysning<Foreldreansvar, KildePdl>>> => {
  return apiClient.get<Grunnlagsopplysning<Foreldreansvar, KildePdl>>(
    `/grunnlag/behandling/${args.behandlingId}/revurdering/HISTORISK_FORELDREANSVAR`
  )
}

export const hentPersonopplysningerForBehandling = async (args: {
  behandlingId: string
  sakType: SakType
}): Promise<ApiResponse<Personopplysninger>> => {
  return apiClient.get<Personopplysninger>(
    `/grunnlag/behandling/${args.behandlingId}/personopplysninger?sakType=${args.sakType}`
  )
}

export const hentPersongalleriSamsvar = async (args: {
  behandlingId: string
}): Promise<ApiResponse<PersongalleriSamsvar>> => {
  return apiClient.get(`/grunnlag/behandling/${args.behandlingId}/opplysning/persongalleri-samsvar`)
}

export const getPersongalleriFraSoeknad = async (args: {
  behandlingId: string
}): Promise<ApiResponse<Grunnlagsopplysning<Persongalleri, KildePdl>>> => {
  return apiClient.get(`/grunnlag/behandling/${args.behandlingId}/PERSONGALLERI_V1`)
}
