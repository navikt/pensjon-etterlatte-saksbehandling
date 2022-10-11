import express, { Request, Response } from 'express'
import { logger } from '../utils/logger'
import { parseJwt } from '../utils/parsejwt'

export const modiaRouter = express.Router() // for å støtte dekoratør for innloggede flater

interface IEnhet {
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
const getSaksbehandler = (req: Request): ISaksbehandler | null => {
  if (process.env.DEVELOPMENT === 'true') {
    /* mulig det bør gjøre kall mot https://modiacontextholder.nais.adeo.no/modiacontextholder/api/decorator */
    return {
      ident: 'Z81549300',
      navn: 'Truls Veileder',
      fornavn: 'Truls',
      etternavn: 'Veileder',
      rolle: 'attestant',
      enheter: [
        {
          enhetId: '0315',
          navn: 'NAV Grünerløkka',
        },
        {
          enhetId: '0316',
          navn: 'NAV Gamle Oslo',
        },
      ],
    }
  }

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
    enheter: [
      // Todo: Hent ut enheter basert på saksbehandler
      {
        enhetId: '0315',
        navn: 'NAV Grünerløkka',
      },
      {
        enhetId: '0316',
        navn: 'NAV Gamle Oslo',
      },
    ],
  }
}

// TODO: endre navn på router og endepunkt
modiaRouter.get('/decorator', (req: Request, res: Response) => {
  try {
    const saksbehandler = getSaksbehandler(req)
    return res.json(saksbehandler)
  } catch (e) {
    logger.info('feil i modiarouter', e)
    res.sendStatus(500)
  }
})
