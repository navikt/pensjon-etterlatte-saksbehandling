import express, { Request, Response } from 'express'
import NodeCache from 'node-cache'
import { logger } from '../monitoring/logger'
import { requireEnvValue } from '../config/config'

const NORG2_URL = requireEnvValue('NORG2_URL')
const TEMA_CACHE_KEY = 'TEMA'

// Cache varer i et døgn (24 timer) siden temaer så og si aldri blir endret.
const cache = new NodeCache({ stdTTL: 60 * 60 * 24 })

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

    const data: Tema[] = await fetch(`${NORG2_URL}/norg2/api/v1/kodeverk/Tema`).then((response) => response.json())

    if (data.length) cache.set(TEMA_CACHE_KEY, data)

    return res.json(data)
  } catch (e) {
    logger.error('Feil oppsto ved henting av temaer fra norg2', e)
    return res.sendStatus(500)
  }
})
