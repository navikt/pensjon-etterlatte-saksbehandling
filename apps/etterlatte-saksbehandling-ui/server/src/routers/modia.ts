import express, { Request, Response } from 'express'
import { parseJwt } from '../utils/parsejwt'
import { getOboToken } from '../middleware/getOboToken'
import fetch from 'node-fetch'
import { logger } from '../utils/logger'
import { ApiConfig } from './../config/config'
import { randomUUID } from 'crypto'

export const modiaRouter = express.Router() // for å støtte dekoratør for innloggede flater

export interface IEnhet {
  enhetId: string
  temaer: string[]
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
  const ident = parsedToken.NAVident

  return {
    ident,
    navn: parsedToken.name,
    fornavn: parsedToken.name.split(', ')[1],
    etternavn: parsedToken.name.split(', ')[0],
    rolle: 'attestant',
    enheter: await hentEnheter(req, ident, bearerToken) || [],
  }
}

const hentEnheter = async (req: Request, ident: string, bearerToken: string): Promise<IEnhet[]> => {
  if (bearerToken) {
    try {
      const oboToken = await getOboToken(bearerToken, ApiConfig.axsys.scope)

      const data: { enheter: IEnhet[] } = await fetch(
        `${ApiConfig.axsys.url!!}/api/v2/tilgang/${ident}?inkluderAlleEnheter=false`,
        {
          headers: {
            Authorization: `Bearer ${oboToken}`,
            'Nav-Consumer-Id': 'etterlatte-saksbehandling-ui',
            'Nav-Call-Id': randomUUID(),
          },
        }
      ).then((response) => response.json())

      return data.enheter.filter((enhet) => enhet.temaer.includes('EYB'))
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
