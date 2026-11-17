import type { ApiConfig, ApiUrlScope } from './config'

export const beregningConfig = {
  brukBehandling: process.env.BRUK_INTERN_BEREGNING === 'true',
}

export const hentBeregningApiConfig = (apiConfig: ApiConfig): ApiUrlScope =>
  beregningConfig.brukBehandling ? apiConfig.behandling : apiConfig.beregning
