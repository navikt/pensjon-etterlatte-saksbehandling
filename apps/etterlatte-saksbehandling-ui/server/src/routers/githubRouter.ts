import express, { Request, Response } from 'express'
import NodeCache from 'node-cache'
import { logger } from '../monitoring/logger'

const RELEASE_CACHE_KEY = 'GITHUB_RELEASES'
const cache = new NodeCache({ stdTTL: 60 * 15 }) // Cache varer i 15 min

export const githubRouter = express.Router()

interface Release {
  id: number
  name: string
  published_at: string
  body: string
}

githubRouter.get('/releases', async (_: Request, res: Response) => {
  try {
    const cachedReleases: Release[] | undefined = cache.get(RELEASE_CACHE_KEY)
    if (cachedReleases?.length) {
      return res.json(cachedReleases)
    }

    const data: Release[] = await fetch(
      `https://api.github.com/repos/navikt/pensjon-etterlatte-saksbehandling/releases?per_page=10`
    ).then((response) => response.json())

    const releases = data.map(({ id, name, published_at, body }) => ({ id, name, published_at, body }))

    if (releases.length) cache.set(RELEASE_CACHE_KEY, releases)

    return res.json(releases)
  } catch (e) {
    logger.error('Feil oppsto ved henting av releases fra Github: ', e)
    return res.sendStatus(500)
  }
})
