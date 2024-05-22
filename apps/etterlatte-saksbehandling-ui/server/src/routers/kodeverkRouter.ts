import express, { Request, Response } from 'express'
import NodeCache from 'node-cache'
import { logger } from '../monitoring/logger'
import { requireEnvValue } from '../config/config'
import { randomUUID } from 'node:crypto'

const KODEVERK_URL = requireEnvValue('KODEVERK_URL')

// Cache varer i et døgn (24 timer) siden temaer så og si aldri blir endret.
const cache = new NodeCache({ stdTTL: 60 * 60 * 24 })

export const kodeverkRouter = express.Router()

interface KodeverkResponse {
  betydninger: Record<string, Betydning[]>
}

interface Betydning {
  gyldigFra: string
  gyldigTil: string
  beskrivelser: Record<string, Beskrivelse>
}

interface Beskrivelse {
  term: string
  tekst: string
}

interface RsKode {
  navn: string
  term: string
}

const ARKIVTEMAER = 'Arkivtemaer'

kodeverkRouter.get(`/${ARKIVTEMAER}`, async (_: Request, res: Response) => {
  try {
    const koder: RsKode[] | undefined = cache.get(ARKIVTEMAER)
    if (!!koder) {
      return res.json(koder)
    }

    const data: RsKode[] = await fetch(
      `${KODEVERK_URL}/api/v1/kodeverk/${ARKIVTEMAER}/koder/betydninger?ekskluderUgyldige=true&spraak=nb`,
      {
        headers: {
          'Nav-Call-Id': randomUUID(),
          'Nav-Consumer-Id': randomUUID(),
        },
      }
    )
      .then((response) => response.json())
      .then((response: KodeverkResponse) => {
        return Object.entries(response.betydninger).map(([key, betydninger]) => ({
          navn: key,
          term: betydninger[0].beskrivelser['nb'].tekst,
        }))
      })

    if (!!data?.length) cache.set(ARKIVTEMAER, data)

    return res.json(data)
  } catch (e) {
    logger.error('Feil oppsto ved henting av temaer fra norg2', e)
    return res.sendStatus(500)
  }
})
