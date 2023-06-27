import express, { Request, Response } from 'express'
import { parseJwt } from '../utils/parsejwt'
import { getOboToken } from '../middleware/getOboToken'
import fetch from 'node-fetch'
import { logger } from '../monitoring/logger'
import { lagEnhetFraString } from '../utils/enhet'

export const modiaRouter = express.Router() // for å støtte dekoratør for innloggede flater

export interface IEnhet {
  enhetId: string
  navn: string
}

export interface ISaksbehandler {
  ident: string
  navn: string
  fornavn: string
  etternavn: string
  enheter: IEnhet[]
  rolle: string //test
}

const getSaksbehandler = async (req: Request): Promise<ISaksbehandler | null> => {
  const auth = req.headers.authorization
  if (!auth) {
    return null
  }

  const bearerToken = auth.split(' ')[1]
  const parsedToken = parseJwt(bearerToken)

  return {
    ident: parsedToken.NAVident,
    navn: parsedToken.name,
    fornavn: parsedToken.name.split(', ')[1],
    etternavn: parsedToken.name.split(', ')[0],
    rolle: 'attestant',
    enheter: await hentEnheter(req, bearerToken),
  }
}

const hentEnheter = async (req: Request, bearerToken: string): Promise<IEnhet[]> => {
  if (bearerToken) {
    try {
      const oboToken = await getOboToken(bearerToken, 'https://graph.microsoft.com/.default')
      const query = 'officeLocation'

      const data = await fetch(`https://graph.microsoft.com/v1.0/me?$select=${query}`, {
        headers: { Authorization: `Bearer ${oboToken}` },
      }).then((response) => response.json())

      const enhet = lagEnhetFraString(data.officeLocation)

      return [enhet]
    } catch (error) {
      logger.info('Feilmelding ved uthenting av enheter', error)
    }
  }
  return []
}

// TODO: endre navn på router og endepunkt
modiaRouter.get('/decorator', async (req: Request, res: Response) => {
  try {
    const saksbehandler = await getSaksbehandler(req)
    return res.json(saksbehandler)
  } catch (e) {
    logger.info('Feil i modiarouter', e)
    res.sendStatus(500)
  }
})
