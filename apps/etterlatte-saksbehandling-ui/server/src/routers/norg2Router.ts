import express, { Request, Response } from 'express'
import NodeCache from 'node-cache'
import { logger } from '../monitoring/logger'

const TEMA_CACHE_KEY = 'TEMA'
const cache = new NodeCache({ stdTTL: 60 * 15 }) // Cache varer i 15 min

export const norg2Router = express.Router()

interface Tema {
  navn: string
  term: string
}

norg2Router.get('/kodeverk/tema', async (_: Request, res: Response) => {
  try {
    const temaCache: Tema[] | undefined = cache.get(TEMA_CACHE_KEY)
    if (temaCache?.length) {
      return res.json(temaCache)
    }

    const data: Tema[] = await fetch('https://norg2.dev-fss-pub.nais.io/norg2/api/v1/kodeverk/Tema').then((response) =>
      response.json()
    )

    if (data.length) cache.set(TEMA_CACHE_KEY, data)

    return res.json(data)
  } catch (e) {
    logger.error('Feil oppsto ved henting av temaer fra norg2', e)
    return res.sendStatus(500)
  }
})
