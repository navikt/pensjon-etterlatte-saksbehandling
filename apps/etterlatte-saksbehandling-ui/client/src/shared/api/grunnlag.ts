import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { PersonMedNavn } from '~shared/types/grunnlag'
import { IPersonResult } from '~components/person/typer'
import { Foreldreansvar } from '~shared/types/Foreldreansvar'

export const hentPersonerISak = async (sakId: number): Promise<ApiResponse<PersonerISakResponse>> => {
  return apiClient.get(`/grunnlag/${sakId}/personer/alle`)
}

export type PersonerISakResponse = {
  personer: Record<string, PersonMedNavn>
}

export const getPerson = async (fnr: string): Promise<ApiResponse<IPersonResult>> => {
  return apiClient.post(`/grunnlag/person`, {
    foedselsnummer: fnr,
  })
}

export const getHistoriskForeldreansvar = (args: { sakId: number }): Promise<ApiResponse<Foreldreansvar>> => {
  return apiClient.get<Foreldreansvar>(`/grunnlag/${args.sakId}/foreldreansvar`)
}
