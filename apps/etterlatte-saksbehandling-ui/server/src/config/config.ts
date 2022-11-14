interface IAppConf {
  port: string | number
}

export const appConf: IAppConf = {
  port: process.env.PORT || 8080,
}

export const AdConfig = {
  clientId: process.env.AZURE_APP_CLIENT_ID,
  clientSecret: process.env.AZURE_APP_CLIENT_SECRET,
  audience: process.env.AZURE_APP_CLIENT_ID,
  issuer: process.env.AZURE_OPENID_CONFIG_ISSUER,
  tokenEndpoint: process.env.AZURE_OPENID_CONFIG_TOKEN_ENDPOINT,
}

export const ApiConfig = {
  vilkaarsvurdering: {
    url: process.env.VILKAARSVURDERING_API_URL,
    scope: 'api://f4cf400f-8ef9-406f-baf1-8218f8f7edac/.default',
  },
  behandling: {
    url: process.env.BEHANDLING_API_URL,
    scope: 'api://59967ac8-009c-492e-a618-e5a0f6b3e4e4/.default',
  },
  api: {
    url: process.env.API_URL,
    scope: 'api://783cea60-43b5-459c-bdd3-de3325bd716a/.default',
  },
  brev: {
    url: process.env.BREV_API_URL,
    scope: 'api://d6add52a-5807-49cd-a181-76908efee836/.default',
  },
}
