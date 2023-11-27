import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Grunnlagsopplysning, PersonMedNavn, Personopplysninger } from '~shared/types/grunnlag'
import { IPersonResult } from '~components/person/typer'
import { Foreldreansvar } from '~shared/types/Foreldreansvar'
import { KildePdl, KildePersondata } from '~shared/types/kilde'
import { IPdlPerson } from '~shared/types/Person'
import { Mottaker } from '~shared/types/Brev'
import { SakType } from '~shared/types/sak'

export const hentPersonerISak = async (sakId: number): Promise<ApiResponse<PersonerISakResponse>> => {
  return apiClient.get(`/grunnlag/sak/${sakId}/personer/alle`)
}

export type PersonerISakResponse = {
  personer: Record<string, PersonMedNavn>
}

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

export const getVergeadresseFraGrunnlag = async (
  behandlingId: string
): Promise<ApiResponse<Grunnlagsopplysning<Mottaker, KildePersondata>>> => {
  return apiClient.get(`/grunnlag/behandling/${behandlingId}/VERGES_ADRESSE`)
}

export const getVergeadresseForPerson = async (
  foedselsnummer: string
): Promise<ApiResponse<Grunnlagsopplysning<Mottaker, KildePersondata>>> => {
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
