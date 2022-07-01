import fetch from 'node-fetch'
import { logger } from '../utils/logger'
import { parseJwt } from '../utils/parsejwt'

export const getOboToken = async (auth: any): Promise<string> => {
  try {
    if (!auth) {
      throw new Error('Ikke autentisert')
    }
    const bearerToken = auth.split(' ')[1]
    //const parsedToken = parseJwt(bearerToken);
    const tokenEndpoint = process.env.AZURE_OPENID_CONFIG_TOKEN_ENDPOINT || ''

    const body: any = {
      client_id: process.env.AZURE_APP_CLIENT_ID,
      client_secret: process.env.AZURE_APP_CLIENT_SECRET,
      scope: ['api://783cea60-43b5-459c-bdd3-de3325bd716a/.default', 'api://d6add52a-5807-49cd-a181-76908efee836'],
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: bearerToken,
      requested_token_use: 'on_behalf_of',
    }

    const response = await fetch(tokenEndpoint, {
      method: 'post',
      body: Object.keys(body)
        .map((key) => encodeURIComponent(key) + '=' + encodeURIComponent(body[key]))
        .join('&'),
      headers: {
        'content-type': 'application/x-www-form-urlencoded',
      },
    })
    if (response.status >= 400) {
      throw new Error('Token-kall feilet')
    }
    const json = await response.json()
    return json.access_token
  } catch (e) {
    throw new Error('Det skjedde en feil ved henting av obo-token')
  }
}
