interface IAppConf {
  port: string | number
}

export const appConf: IAppConf = {
  port: process.env.PORT || 8080,
}

export const AdConfig = {
  clientId: requireEnvValue('AZURE_APP_CLIENT_ID'),
  clientSecret: requireEnvValue('AZURE_APP_CLIENT_SECRET'),
  audience: requireEnvValue('AZURE_APP_CLIENT_ID'),
  issuer: requireEnvValue('AZURE_OPENID_CONFIG_ISSUER'),
  tokenEndpoint: requireEnvValue('AZURE_OPENID_CONFIG_TOKEN_ENDPOINT'),
}

function hentApiConfigFraEnv() {
  const kjorerIGCP = process.env.NAIS_CLUSTER_NAME // NAIS_CLUSTER_NAME er satt i gcp
  if (kjorerIGCP) {
    return API_CONFIG_FROM_ENV()
  } else {
    return LOKAL_API_CONFIG()
  }
}

export const FeatureToggleConfig = {
  host: process.env.UNLEASH_SERVER_API_URL || '',
  token: process.env.UNLEASH_SERVER_API_TOKEN || '',
  applicationName: process.env.NAIS_APP_NAME || 'etterlatte-saksbehandling-ui',
}

const LOKAL_API_CONFIG = () => {
  return {
    vilkaarsvurdering: {
      url: process.env.VILKAARSVURDERING_API_URL || 'https://etterlatte-vilkaarsvurdering.intern.dev.nav.no',
      scope:
        process.env.VILKAARSVURDERING_API_SCOPE || 'api://dev-gcp.etterlatte.etterlatte-vilkaarsvurdering/.default',
    },
    behandling: {
      url: process.env.BEHANDLING_API_URL || 'https://etterlatte-behandling.intern.dev.nav.no',
      scope: process.env.BEHANDLING_API_SCOPE || 'api://dev-gcp.etterlatte.etterlatte-behandling/.default',
    },
    grunnlag: {
      url: process.env.GRUNNLAG_API_URL || 'https://etterlatte-grunnlag.intern.dev.nav.no',
      scope: process.env.GRUNNLAG_API_SCOPE || 'api://dev-gcp.etterlatte.etterlatte-grunnlag/.default',
    },
    brev: {
      url: process.env.BREV_API_URL || 'https://etterlatte-brev-api.intern.dev.nav.no',
      scope: process.env.BREV_API_SCOPE || 'api://dev-gcp.etterlatte.etterlatte-brev-api/.default',
    },
    beregning: {
      url: process.env.BEREGNING_API_URL || 'https://etterlatte-beregning.intern.dev.nav.no',
      scope: process.env.BEREGNING_API_SCOPE || 'api://dev-gcp.etterlatte.etterlatte-beregning/.default',
    },
    vedtak: {
      url: process.env.VEDTAK_API_URL || 'https://etterlatte-vedtaksvurdering.intern.dev.nav.no',
      scope: process.env.VEDTAK_API_SCOPE || 'api://dev-gcp.etterlatte.etterlatte-vedtaksvurdering/.default',
    },
    trygdetid: {
      url: process.env.TRYGDETID_API_URL || 'https://etterlatte-trygdetid.intern.dev.nav.no',
      scope: process.env.TRYGDETID_API_SCOPE || 'api://dev-gcp.etterlatte.etterlatte-trygdetid/.default',
    },
  }
}

function requireEnvValue(key: string): string {
  const envValue = process.env[key]
  if (envValue) {
    return envValue
  }
  throw new Error(`Env is missing required key ${key}`)
}

const API_CONFIG_FROM_ENV = () => {
  return {
    vilkaarsvurdering: {
      url: requireEnvValue('VILKAARSVURDERING_API_URL'),
      scope: requireEnvValue('VILKAARSVURDERING_API_SCOPE'),
    },
    behandling: {
      url: requireEnvValue('BEHANDLING_API_URL'),
      scope: requireEnvValue('BEHANDLING_API_SCOPE'),
    },
    grunnlag: {
      url: requireEnvValue('GRUNNLAG_API_URL'),
      scope: requireEnvValue('GRUNNLAG_API_SCOPE'),
    },
    brev: {
      url: requireEnvValue('BREV_API_URL'),
      scope: requireEnvValue('BREV_API_SCOPE'),
    },
    beregning: {
      url: requireEnvValue('BEREGNING_API_URL'),
      scope: requireEnvValue('BEREGNING_API_SCOPE'),
    },
    vedtak: {
      url: requireEnvValue('VEDTAK_API_URL'),
      scope: requireEnvValue('VEDTAK_API_SCOPE'),
    },
    trygdetid: {
      url: requireEnvValue('TRYGDETID_API_URL'),
      scope: requireEnvValue('TRYGDETID_API_SCOPE'),
    },
  }
}

export const ApiConfig = hentApiConfigFraEnv()

export const ClientConfig = {
  gosysUrl: requireEnvValue('GOSYS_URL'),
}
