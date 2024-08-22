import { apiClient, ApiResponse } from '~shared/api/apiClient'

export interface DigitalKontaktinformasjon {
  personident: string
  aktiv: boolean
  kanVarsles?: boolean
  reservert?: boolean
  spraak?: string
  epostadresse?: string
  mobiltelefonnummer?: string
  sikkerDigitalPostkasse?: SikkerDigitalPostkasse
}

interface SikkerDigitalPostkasse {
  adresse: string
  leverandoerAdresse: string
  leverandoerSertifikat: string
}

export const hentKontaktinformasjonKRR = async (fnr: string): Promise<ApiResponse<DigitalKontaktinformasjon>> =>
  apiClient.post(`/krr`, { foedselsnummer: fnr })
