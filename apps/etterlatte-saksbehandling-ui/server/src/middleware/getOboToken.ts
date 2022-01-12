import fetch from 'node-fetch'
import { logger } from '../utils/logger'
import { parseJwt } from '../utils/parsejwt'

/*
    POST /oauth2/v2.0/token HTTP/1.1
    Host: login.microsoftonline.com/<tenant>
    Content-Type: application/x-www-form-urlencoded

    grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer
    client_id=535fb089-9ff3-47b6-9bfb-4f1264799865
    &client_secret=sampleCredentia1s
    &assertion=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6InowMzl6ZHNGdWl6cEJmQlZLMVRuMjVRSFlPMCJ9.eyJhdWQiOiIyO{a lot of characters here}
    &scope=https://graph.microsoft.com/user.read+offline_access
    &requested_token_use=on_behalf_of
    */

export const getOboToken = async (auth: any): Promise<string> => {
  try {
    if (!auth) {
      throw new Error('Ikke autentisert')
    }
    const bearerToken = auth.split(' ')[1]
    //const parsedToken = parseJwt(bearerToken);
    const tokenEndpoint = process.env.AZURE_OPENID_CONFIG_TOKEN_ENDPOINT || ''
    console.log(process.env);
    console.log('token-endpoint: ', tokenEndpoint)
    console.log('token', bearerToken)


    const response = await fetch(tokenEndpoint, {
      method: 'post',
      body: JSON.stringify({
        client_id: process.env.AZURE_APP_CLIENT_ID,
        client_secret: process.env.AZURE_APP_CLIENT_SECRET,
        scope: 'api://783cea60-43b5-459c-bdd3-de3325bd716a/.default',
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion: bearerToken,
        requested_token_use: 'on_behalf_of',
      }),
      headers: {
        'content-type': 'application/json',
      },
    })
    console.log('response: ', response)
    if (response.status >= 400) {
      throw new Error('Token-kall feilet')
    }
    const json = await response.json()
    console.log('token?', json)
    return json.access_token
  } catch (e) {
    console.log('error:', e)
    throw new Error('Det skjedde en feil ved henting av obo-token')
  }
}
