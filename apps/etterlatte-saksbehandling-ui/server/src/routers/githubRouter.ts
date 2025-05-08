import express, { Request, Response } from 'express'
import NodeCache from 'node-cache'
import { logger } from '../monitoring/logger'

const RELEASE_CACHE_KEY = 'GITHUB_RELEASES'
const cache = new NodeCache({ stdTTL: 60 * 60 }) // Cache varer i 60 min

export const githubRouter = express.Router()

interface Release {
  id: number
  name: string
  published_at: string
  draft: boolean
  body: string
}

interface ErrorResponse {
  message: string
  documentation_url?: string
}

githubRouter.get('/releases', async (_: Request, res: Response): Promise<void> => {
  try {
    const cachedReleases: Release[] | undefined = cache.get(RELEASE_CACHE_KEY)
    if (cachedReleases?.length) {
      res.json(cachedReleases)
      return
    }

    const data = await fetch(
      `https://api.github.com/repos/navikt/pensjon-etterlatte-saksbehandling/releases?per_page=10`
    ).then((response) => response.json())

    if (Array.isArray(data)) {
      const releases = (data as Release[])
        .filter(({ draft }) => !draft)
        .map(({ id, name, published_at, body }) => ({ id, name, published_at, body }))

      if (releases.length) cache.set(RELEASE_CACHE_KEY, releases)

      res.json(releases)
    } else if (typeof data === 'object') {
      logger.warn((data as ErrorResponse)?.message || 'Data fra Github er ikke forventet type objekt')
      res.json([])
    } else {
      logger.warn('Ukjent respons fra Github')
      res.json([])
    }
  } catch (e) {
    logger.error('Feil oppsto ved henting av releases fra Github: ', e)
    res.sendStatus(500)
  }
})
