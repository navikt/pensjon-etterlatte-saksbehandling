export interface Token {
  name: string
  NAVident: string
  groups: string[]
  aud: string
  iss: string
  exp: number
  iat: number
}

export const parseJwt = (token: string): Token => {
  const base64Payload = token.split('.')[1]
  const payload = Buffer.from(base64Payload, 'base64')

  return JSON.parse(payload.toString())
}
