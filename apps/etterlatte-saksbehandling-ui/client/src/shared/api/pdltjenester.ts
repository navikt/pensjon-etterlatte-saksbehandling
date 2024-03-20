import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IPdlPersonNavn } from '~shared/types/Person'

/**
 * Hent navn til person med ident.
 * Identen kan være:
 * - Fødselsnummer
 * - AktørID
 * - NPID
 **/
export const hentPersonNavn = async (ident: string): Promise<ApiResponse<IPdlPersonNavn>> =>
  apiClient.post(`/pdltjenester/person/navn`, { ident })
