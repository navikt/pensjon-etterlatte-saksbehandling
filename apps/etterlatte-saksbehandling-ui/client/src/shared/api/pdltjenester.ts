import { apiClient, ApiResponse } from '~shared/api/apiClient'
import { IPdlPersonNavnFoedselsAar } from '~shared/types/Person'
import { SakType } from '~shared/types/sak'
import { FamilieOpplysninger } from '~shared/types/familieOpplysninger'

/**
 * Hent navn til person med ident.
 * Identen kan være:
 * - Fødselsnummer
 * - AktørID
 * - NPID
 **/

export const hentPersonNavnFoedselsdatoOgFoedselsnummer = async (
  ident: string
): Promise<ApiResponse<IPdlPersonNavnFoedselsAar>> => apiClient.post(`/pdltjenester/person/navn-foedselsaar`, { ident })

export const hentFamilieOpplysninger = async (args: {
  ident: string
  sakType: SakType
}): Promise<ApiResponse<FamilieOpplysninger>> => apiClient.post('/pdltjenester/person/familieOpplysninger', args)
