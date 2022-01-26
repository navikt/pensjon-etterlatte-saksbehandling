interface IAppConf {
  port: string | number
}
export const appConf: IAppConf = {
    port: process.env.PORT || 8080,
};

export const AdConfig = {
  audience: process.env.AZURE_APP_CLIENT_ID,
  issuer: process.env.AZURE_OPENID_CONFIG_ISSUER
}