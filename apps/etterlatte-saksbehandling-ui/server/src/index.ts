import express, { Request, Response } from 'express'
import path from 'path'
import { ApiConfig, appConf, ClientConfig } from './config/config'
import { authenticateUser } from './middleware/auth'
import { mockRouter } from './routers/mockRouter'
import { logger } from './monitoring/logger'
import { requestLogger } from './middleware/logging'
import { tokenMiddleware } from './middleware/getOboToken'
import { proxy } from './middleware/proxy'
import { loggerRouter } from './routers/loggerRouter'
import prometheus from './monitoring/prometheus'
import { sanitize } from './utils/sanitize'
import { unleash } from './utils/unleash'
import { innloggetBrukerRouter } from './routers/innloggetBrukerRouter'

logger.info(`environment: ${process.env.NODE_ENV}`)

const clientPath = path.resolve(__dirname, '..', 'client')
const isDev = process.env.NODE_ENV !== 'production'

const app = express()
app.set('trust proxy', 1)
app.use('/', express.static(clientPath))
app.use(requestLogger(isDev))

app.use(['/health/isAlive', '/health/isReady'], (req: Request, res: Response) => {
  res.send('OK')
})

app.get('/metrics', async (req: Request, res: Response) => {
  res.set('Content-Type', prometheus.register.contentType)
  res.end(await prometheus.register.metrics())
})

if (isDev) {
  logger.info('Mocking all endpoints')
  app.use('/api', mockRouter)
} else {
  app.use('/api/logg', loggerRouter)

  app.post('/api/feature', express.json(), (req: Request, res: Response) => {
    const toggles: string[] = req.body.features

    const PAA = 'PAA'
    const AV = 'AV'
    const UDEFINERT = 'UDEFINERT'
    const HENTING_FEILA = 'HENTING_FEILA'

    const isEnabled = (toggle: string): String => {
      if (!unleash) {
        return UDEFINERT
      }
      try {
        if (unleash.isEnabled(toggle, undefined)) {
          return PAA
        } else {
          return AV
        }
      } catch (e) {
        logger.error({
          message: `Fikk feilmelding fra Unleash for toggle ${sanitize(toggle)}, bruker defaultverdi false`,
          error: JSON.stringify(e),
        })
        return HENTING_FEILA
      }
    }

    res.json(
      toggles.map((toggle) => {
        const enabled = isEnabled(toggle)

        logger.info(`${sanitize(toggle)} enabled: ${enabled}`)

        return {
          toggle: toggle,
          enabled: enabled,
        }
      })
    )
  })

  app.use(authenticateUser) // Alle ruter etter denne er authenticated
  app.use('/api/innlogget', innloggetBrukerRouter)

  app.use(
    '/api/vilkaarsvurdering',
    tokenMiddleware(ApiConfig.vilkaarsvurdering.scope),
    proxy(ApiConfig.vilkaarsvurdering.url)
  )

  app.use(
    [
      '/api/behandling',
      '/api/behandlinger/:sakid/manueltopphoer',
      '/api/personer',
      '/api/revurdering/:sakid',
      '/api/stoettederevurderinger',
      '/api/grunnlagsendringshendelse/:sakid/institusjon',
      '/api/sak/:sakid',
      '/api/institusjonsoppholdbegrunnelse/:sakid',
      '/api/oppgaver',
      '/api/klage',
      '/api/tilbakekreving',
      '/api/generellbehandling',
      '/api/sjekkliste',
      '/api/bosattutland',
    ],
    tokenMiddleware(ApiConfig.behandling.scope),
    proxy(ApiConfig.behandling.url)
  )

  app.use('/api/grunnlag', tokenMiddleware(ApiConfig.grunnlag.scope), proxy(ApiConfig.grunnlag.url))

  app.use('/api/beregning', tokenMiddleware(ApiConfig.beregning.scope), proxy(ApiConfig.beregning.url))

  app.use('/api/vedtak', tokenMiddleware(ApiConfig.vedtak.scope), proxy(ApiConfig.vedtak.url))

  app.use('/api/trygdetid', tokenMiddleware(ApiConfig.trygdetid.scope), proxy(ApiConfig.trygdetid.url))

  app.use(['/api/brev', '/api/dokumenter'], tokenMiddleware(ApiConfig.brev.scope), proxy(ApiConfig.brev.url))
}

app.get('/api/config', (_req: Request, res: Response) => res.send(ClientConfig))

app.use(/^(?!.*\/(internal|static)\/).*$/, (_req: Request, res: Response) => {
  return res.sendFile(`${clientPath}/index.html`)
})

app.listen(appConf.port, () => {
  logger.info(`Server running on port ${appConf.port}`)
})
