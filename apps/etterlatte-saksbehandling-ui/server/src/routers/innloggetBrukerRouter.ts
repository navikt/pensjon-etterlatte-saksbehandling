import express, { Request, Response } from 'express'
import { parseJwt } from '../utils/parsejwt'
import { getOboToken } from '../middleware/getOboToken'
import fetch from 'node-fetch'
import { logger } from '../monitoring/logger'
import { lagEnhetFraString } from '../utils/enhet'

export const innloggetBrukerRouter = express.Router()

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
  kanAttestere: boolean
  leseTilgang: boolean
  skriveTilgang: boolean
}

const kanAttestere = (groups: string[]): boolean => {
  if (!process.env.NAIS_CLUSTER_NAME || process.env.NAIS_CLUSTER_NAME === 'dev-gcp') {
    return groups.includes('63f46f74-84a8-4d1c-87a8-78532ab3ae60') // 0000-GA-PENSJON_ATTESTERING (DEV)
  } else {
    return groups.includes('11053fd7-e674-4552-9a88-f9fcedfa70b3') // 0000-GA-PENSJON_ATTESTERING (PROD)
  }
}

const NKSEnheterKunLes = ['4101', '4116', '4118'] //Les
const skriveOgLesEnheter = ['2103', '4883', '0001', '4815', '4862', '4817', '4808'] //Skriv
const devenhetZBruker = ['2970']

const harSkrivetilgang = (enheter: IEnhet[]) => {
  if (!process.env.NAIS_CLUSTER_NAME || process.env.NAIS_CLUSTER_NAME === 'dev-gcp') {
    return enheter.some((e) => skriveOgLesEnheter.includes(e.enhetId) || devenhetZBruker.includes(e.enhetId))
  }
  return enheter.some((e) => skriveOgLesEnheter.includes(e.enhetId))
}

const harLesetilgang = (enheter: IEnhet[]) => {
  if (!process.env.NAIS_CLUSTER_NAME || process.env.NAIS_CLUSTER_NAME === 'dev-gcp') {
    return enheter.some(
      (e) =>
        NKSEnheterKunLes.includes(e.enhetId) ||
        skriveOgLesEnheter.includes(e.enhetId) ||
        devenhetZBruker.includes(e.enhetId)
    )
  }

  return enheter.some((e) => NKSEnheterKunLes.includes(e.enhetId) || skriveOgLesEnheter.includes(e.enhetId))
}

const getSaksbehandler = async (req: Request): Promise<ISaksbehandler | null> => {
  const auth = req.headers.authorization
  if (!auth) {
    return null
  }

  const bearerToken = auth.split(' ')[1]
  const parsedToken = parseJwt(bearerToken)

  const enheter = await hentEnheter(req, bearerToken)
  return {
    ident: parsedToken.NAVident,
    navn: parsedToken.name,
    fornavn: parsedToken.name.split(', ')[1],
    etternavn: parsedToken.name.split(', ')[0],
    enheter: enheter,
    kanAttestere: kanAttestere(parsedToken.groups),
    leseTilgang: harLesetilgang(enheter),
    skriveTilgang: harSkrivetilgang(enheter),
  }
}

const hentEnheter = async (_: Request, bearerToken: string): Promise<IEnhet[]> => {
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

innloggetBrukerRouter.get('/', async (req: Request, res: Response) => {
  try {
    const saksbehandler = await getSaksbehandler(req)

    res.json(saksbehandler)
  } catch (e) {
    logger.info('Feil i innloggetBrukerRouter', e)
    res.sendStatus(500)
  }
  return
})
