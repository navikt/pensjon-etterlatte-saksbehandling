
export interface Token {
  name: string;
  NAVident: string;
  aud: string;
  iss: string;
  exp: number;
  iat: number;
}

export const parseJwt = (token: string): Token => {
  var base64Payload = token.split('.')[1];
  var payload = Buffer.from(base64Payload, 'base64');
  return JSON.parse(payload.toString());
};