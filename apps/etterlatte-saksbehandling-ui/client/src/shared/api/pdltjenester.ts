import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IPdlPersonNavn } from '~shared/types/Person'
import { SakType } from '~shared/types/sak'
import { Personopplysninger } from '~shared/types/personopplysninger'

/**
 * Hent navn til person med ident.
 * Identen kan være:
 * - Fødselsnummer
 * - AktørID
 * - NPID
 **/
export const hentPersonNavn = async (ident: string): Promise<ApiResponse<IPdlPersonNavn>> =>
  apiClient.post(`/pdltjenester/person/navn`, { ident })

export const hentPersonopplysninger = async (args: {
  ident: string
  sakType: SakType
}): Promise<ApiResponse<Personopplysninger>> => apiClient.post('/pdltjenester/person/opplysninger', args)
