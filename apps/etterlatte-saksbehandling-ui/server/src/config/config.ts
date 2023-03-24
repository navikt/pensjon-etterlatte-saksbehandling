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

function hentApiConfigFraEnv(): typeof API_CONFIG_FROM_ENV {
  const kjoererLokalt = !process.env.NAIS_CLUSTER_NAME // NAIS_CLUSTER_NAME er satt i gcp
  if (kjoererLokalt) {
    return LOKAL_API_CONFIG
  }
  return API_CONFIG_FROM_ENV
}

const LOKAL_API_CONFIG = {
  vilkaarsvurdering: {
    url: 'https://etterlatte-vilkaarsvurdering.dev.intern.nav.no',
    scope: 'api://f4cf400f-8ef9-406f-baf1-8218f8f7edac/.default',
  },
  behandling: {
    url: 'https://etterlatte-behandling.dev.intern.nav.no',
    scope: 'api://59967ac8-009c-492e-a618-e5a0f6b3e4e4/.default',
  },
  grunnlag: {
    url: 'https://etterlatte-grunnlag.dev.intern.nav.no',
    scope: 'api://ce96a301-13db-4409-b277-5b27f464d08b/.default',
  },
  brev: {
    url: 'https://etterlatte-brev-api.dev.intern.nav.no',
    scope: 'api://d6add52a-5807-49cd-a181-76908efee836/.default',
  },
  beregning: {
    url: 'https://etterlatte-beregning.dev.intern.nav.no',
    scope: 'api://b07cf335-11fb-4efa-bd46-11f51afd5052/.default',
  },
  vedtak: {
    url: 'https://etterlatte-vedtaksvurdering.dev.intern.nav.no',
    scope: 'api://069b1b2c-0a06-4cc9-8418-f100b8ff71be/.default',
  },
  trygdetid: {
    url: 'https://etterlatte-trygdetid.dev.intern.nav.no',
    scope: 'api://8385435e-f3d7-45ec-be59-ebd1c71df735/.default',
  },
} as const

function requireEnvValue(key: string): string {
  const envValue = process.env[key]
  if (envValue) {
    return envValue
  }
  throw new Error(`Env is missing required key ${key}`)
}

const API_CONFIG_FROM_ENV = {
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
} as const

export const ApiConfig = hentApiConfigFraEnv()
