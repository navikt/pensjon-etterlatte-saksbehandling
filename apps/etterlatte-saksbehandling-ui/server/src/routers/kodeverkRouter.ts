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

kodeverkRouter.get('/:kodeverkNavn', async (req: Request, res: Response) => {
  try {
    logger.info('req.params', req.params)

    const kodeverkNavn = req.params.kodeverkNavn
    if (!kodeverkNavn) {
      return res.status(500).send('Param :kodeverkNavn mangler')
    }

    const kodeverkResponse: KodeverkResponse | undefined = cache.get(kodeverkNavn)
    if (!!kodeverkResponse) {
      return res.json(kodeverkResponse)
    }

    const data: RsKode[] = await fetch(
      `${KODEVERK_URL}/api/v1/kodeverk/${kodeverkNavn}/koder/betydninger?ekskluderUgyldige=true&spraak=nb`,
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

    if (!!data?.length) cache.set(kodeverkNavn, data)

    return res.json(data)
  } catch (e) {
    logger.error('Feil oppsto ved henting av temaer fra norg2', e)
    return res.sendStatus(500)
  }
})
