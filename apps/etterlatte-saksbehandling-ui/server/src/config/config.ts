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
    url: process.env.VILKAARSVURDERING_API_URL || 'https://etterlatte-vilkaarsvurdering.dev.intern.nav.no',
    scope: process.env.VILKAARSVURDERING_API_SCOPE || 'api://f4cf400f-8ef9-406f-baf1-8218f8f7edac/.default',
  },
  behandling: {
    url: process.env.BEHANDLING_API_URL || 'https://etterlatte-behandling.dev.intern.nav.no',
    scope: process.env.BEHANDLING_API_SCOPE || 'api://59967ac8-009c-492e-a618-e5a0f6b3e4e4/.default',
  },
  grunnlag: {
    url: process.env.GRUNNLAG_API_URL || 'https://etterlatte-grunnlag.dev.intern.nav.no',
    scope: process.env.GRUNNLAG_API_SCOPE || 'api://ce96a301-13db-4409-b277-5b27f464d08b/.default',
  },
  brev: {
    url: process.env.BREV_API_URL || 'https://etterlatte-brev-api.dev.intern.nav.no',
    scope: process.env.BREV_API_SCOPE || 'api://d6add52a-5807-49cd-a181-76908efee836/.default',
  },
  beregning: {
    url: process.env.BEREGNING_API_URL || 'https://etterlatte-beregning.dev.intern.nav.no',
    scope: process.env.BEREGNING_API_SCOPE || 'api://b07cf335-11fb-4efa-bd46-11f51afd5052/.default',
  },
  vedtak: {
    url: process.env.VEDTAK_API_URL || 'https://etterlatte-vedtaksvurdering.dev.intern.nav.no',
    scope: process.env.VEDTAK_API_SCOPE || 'api://069b1b2c-0a06-4cc9-8418-f100b8ff71be/.default',
  },
  trygdetid: {
    url: process.env.TRYGDETID_API_URL || 'https://etterlatte-trygdetid.dev.intern.nav.no',
    scope: process.env.TRYGDETID_API_SCOPE || 'api://8385435e-f3d7-45ec-be59-ebd1c71df735/.default',
  },
}
