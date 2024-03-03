import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IPdlPersonNavn } from '~shared/types/Person'

export const hentPersonNavn = async (foedselsnummer: string): Promise<ApiResponse<IPdlPersonNavn>> =>
  apiClient.post(`/pdltjenester/person/navn`, { foedselsnummer })
