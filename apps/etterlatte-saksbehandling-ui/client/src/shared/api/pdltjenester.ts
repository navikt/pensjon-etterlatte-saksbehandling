import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IPdlPersonNavnFoedsel } from '~shared/types/Person'
import { SakType } from '~shared/types/sak'
import { FamilieOpplysninger } from '~shared/types/familieOpplysninger'

/**
 * Hent navn til person med ident.
 * Identen kan være:
 * - Fødselsnummer
 * - AktørID
 * - NPID
 **/

export const hentPersonNavnogFoedsel = async (ident: string): Promise<ApiResponse<IPdlPersonNavnFoedsel>> =>
  apiClient.post(`/pdltjenester/person/navn-foedsel`, { ident })

export const hentFamilieOpplysninger = async (args: {
  ident: string
  sakType: SakType
}): Promise<ApiResponse<FamilieOpplysninger>> => apiClient.post('/pdltjenester/person/familieOpplysninger', args)
