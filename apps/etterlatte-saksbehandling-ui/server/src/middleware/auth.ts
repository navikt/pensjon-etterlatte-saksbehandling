import { NextFunction, Request, Response } from 'express'
import { AdConfig } from '../config/config'
import { utcSecondsSinceEpoch } from '../utils/date'
import { logger } from '../utils/logger'
import { parseJwt } from '../utils/parsejwt'

export const hasBeenIssued = (issuedAtTime: number) => issuedAtTime < utcSecondsSinceEpoch() // sjekker at issued-date har vært
export const hasExpired = (expires: number) => expires < utcSecondsSinceEpoch()

export const authenticateUser = (req: Request, res: Response, next: NextFunction) => {
  /* NAIS notes
        Token Validation
        Your application should also validate the claims and signature for the Azure AD JWT access_token attached by the sidecar.
        That is, validate the standard claims such as iss, iat, exp, and aud.
        aud must be equal to your application's client ID in Azure AD.
    */

  const auth = req.headers.authorization
  if (!auth) {
    return res.redirect('/oauth2/login')
  }
  const bearerToken = auth.split(' ')[1]
  const parsedToken = parseJwt(bearerToken)

  try {
    if (parsedToken.aud !== AdConfig.audience) {
      throw new Error('Ugyldig audience')
    }
    if (parsedToken.iss !== AdConfig.issuer) {
      throw new Error('Ugyldig issuer')
    }
    if (!hasBeenIssued(parsedToken.iat)) {
      throw new Error(`Ugyldig iat: ${parsedToken.iat}`)
    }
    if (hasExpired(parsedToken.exp)) {
      throw new Error('Token expired')
    }
  } catch (e) {
    logger.error('Feil ved validering av token', e)
    return res.status(401).send('ugyldig token')
  }

  const NAVident = parsedToken.NAVident
  const cluster = process.env.NAIS_CLUSTER_NAME
  logger.info(`Navident logger på ${NAVident} cluster-name ${cluster}`)

  return next()
}
