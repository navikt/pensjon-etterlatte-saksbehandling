import express, { Request, Response } from 'express'
import { parseJwt } from '../utils/parsejwt'
import { getOboToken } from "../middleware/getOboToken"
import fetch from 'node-fetch'
import { logger } from '../utils/logger'

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

const hentBrukerprofil = async (req: Request): Promise<ISaksbehandler | null> => {

    const auth = req.headers.authorization
    if (!auth) {
        return null
    }
    const bearerToken = auth.split(' ')[1]


    if (bearerToken) {
        // sjekk valid token
        try {
            const oboToken = await getOboToken( req.headers.authorization, 'https://graph.microsoft.com/.default');
            logger.info("----------- Henter info om saksbehandler ------------")
            logger.info("URL: ", process.env.GRAPH_URL)
            // hent bruker fra graph.microsoft.com
            const query = "officeLocation";
            //const graphUrl = `${process.env.GRAPH_URL}?$select=${query}`;

            const data = await fetch(`https://graph.microsoft.com/v1.0/me?$select=${query}`, { headers: {"Authorization" : `Bearer ${oboToken}`} })
              .then(response => response.json())
            const parsedToken = parseJwt(bearerToken)

            const user =
              {
                  ident: parsedToken.NAVident,
                  navn: parsedToken.name,
                  fornavn: parsedToken.name.split(', ')[1],
                  etternavn: parsedToken.name.split(', ')[0],
                  rolle: 'attestant',
                  navIdent: data.onPremisesSamAccountName,
                  enheter: [data.officeLocation]
              }

              logger.info("-------- Bruker --------", user)

            return user
        } catch (error) {
            logger.info("Feilmelding ved uthenting av enheter", error);
        }
    }
    return null
};

// TODO: endre navn på router og endepunkt
modiaRouter.get('/decorator', (req: Request, res: Response) => {
  try {
    const saksbehandler = getSaksbehandler(req)
    hentBrukerprofil(req)
    return res.json(saksbehandler)
  } catch (e) {
    logger.info('Feil i modiarouter', e)
    res.sendStatus(500)
  }
})
