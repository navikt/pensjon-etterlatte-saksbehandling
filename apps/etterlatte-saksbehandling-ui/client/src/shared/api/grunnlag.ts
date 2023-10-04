import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { Grunnlagsopplysning, PersonMedNavn } from '~shared/types/grunnlag'
import { IPersonResult } from '~components/person/typer'
import { Foreldreansvar } from '~shared/types/Foreldreansvar'
import { KildePdl } from '~shared/types/kilde'

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

export const getHistoriskForeldreansvar = (args: {
  sakId: number
}): Promise<ApiResponse<Grunnlagsopplysning<Foreldreansvar, KildePdl>>> => {
  return apiClient.get<Grunnlagsopplysning<Foreldreansvar, KildePdl>>(
    `/grunnlag/sak/${args.sakId}/revurdering/HISTORISK_FORELDREANSVAR`
  )
}
