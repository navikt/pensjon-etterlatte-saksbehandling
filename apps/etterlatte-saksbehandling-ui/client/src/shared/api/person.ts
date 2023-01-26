import { apiClient, ApiResponse } from './apiClient'
import { IPersonResult } from '~components/person/typer'

export const INVALID_FNR = (input: string | undefined) => /[a-zA-Z]/g.test(input ?? '') || input?.length !== 11

export const getPerson = async (fnr: string): Promise<ApiResponse<IPersonResult>> => {
  return apiClient.post(`/grunnlag/person`, {
    foedselsnummer: fnr,
  })
}
