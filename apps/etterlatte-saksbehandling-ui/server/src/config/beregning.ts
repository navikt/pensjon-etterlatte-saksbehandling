import type { ApiConfig, ApiUrlScope } from './config'

export const beregningConfig = {
  brukBehandling: false,
}

export const hentBeregningApiConfig = (apiConfig: ApiConfig): ApiUrlScope =>
  beregningConfig.brukBehandling ? apiConfig.behandling : apiConfig.beregning
