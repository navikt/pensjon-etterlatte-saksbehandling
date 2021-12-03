interface IAppConf {
  port: string | number
}
export const appConf: IAppConf = {
    port: process.env.PORT || 8080,
};


interface IOBOConf {
  client_id: string | undefined;
  scope: string;
  redirect_uri: string;
  grant_type: string;
  client_secret: string;
  code: string;
}

export const azureConf: IOBOConf = {
  client_id: process.env.clientID,
  redirect_uri: "",
  scope: "",
  grant_type: "",
  client_secret: "",
  code: ""
}