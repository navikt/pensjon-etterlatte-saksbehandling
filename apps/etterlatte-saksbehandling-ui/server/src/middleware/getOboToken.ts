import fetch from 'node-fetch'
import { logger } from '../monitoring/logger'
import { NextFunction, Request, Response } from 'express'
import { AdConfig } from '../config/config'
import { getTokenInCache, setTokenInCache } from '../cache'

interface OboResponse {
  access_token: string
  expires_in: number
}

export const tokenMiddleware = (scope: string) => async (req: Request, res: Response, next: NextFunction) => {
  const bearerToken = req.headers?.authorization?.split(' ')[1]

  if (!bearerToken) return next(new Error('Ikke autentisert'))

  getOboToken(bearerToken, scope)
    .then((token) => {
      res.locals.token = token
      return next()
    })
    .catch((error) => next(error))
}

export const getOboToken = async (bearerToken: string, scope: string): Promise<string> => {
  const cacheKey = bearerToken + '.' + scope

  const [cacheHit, tokenFromCache] = getTokenInCache(cacheKey)
  logger.debug(`getOboToken: cache hit for scope=${scope}? ${cacheHit}`)
  if (cacheHit) {
    return tokenFromCache
  }

  try {
    const body: Record<string, string> = {
      client_id: AdConfig.clientId,
      client_secret: AdConfig.clientSecret,
      scope,
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: bearerToken,
      requested_token_use: 'on_behalf_of',
    }

    const response = await fetch(AdConfig.tokenEndpoint, {
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

    const json = (await response.json()) as OboResponse

    setTokenInCache(cacheKey, json['access_token'], json['expires_in'])

    return json['access_token']
  } catch (e) {
    logger.info('Feil ved henting av obo-token: ', e)
    throw new Error('Det skjedde en feil ved henting av obo-token')
  }
}
