import express, { Request, Response } from 'express'
import path from 'path'
import { appConf, ApiConfig } from './config/config'
import { authenticateUser } from './middleware/auth'
import { mockRouter } from './routers/mockRouter'
import { modiaRouter } from './routers/modia'
import { logger } from './utils/logger'
import { requestLogger } from './middleware/logging'
import { tokenMiddleware } from './middleware/getOboToken'
import { proxy } from './middleware/proxy'
import { loggerRouter } from './routers/loggerRouter'

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

if (isDev) {
  logger.info('Mocking all endpoints')
  app.use('/api', mockRouter)
} else {
  app.use('/api/logg', loggerRouter)

  app.use(authenticateUser) // Alle ruter etter denne er authenticated
  app.use('/api/modiacontextholder/', modiaRouter) // bytte ut med etterlatte-innlogget?

  app.use(
    '/api/vilkaarsvurdering',
    tokenMiddleware(ApiConfig.vilkaarsvurdering.scope),
    proxy(ApiConfig.vilkaarsvurdering.url)
  )

  app.use(
    [
      '/api/behandling',
      '/api/behandlinger/:sakid/manueltopphoer',
      '/api/oppgaver',
      '/api/personer',
      '/api/:sakid/revurdering',
      '/api/stoettederevurderinger',
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

app.use(/^(?!.*\/(internal|static)\/).*$/, (req: Request, res: Response) => {
  return res.sendFile(`${clientPath}/index.html`)
})

app.listen(appConf.port, () => {
  logger.info(`Server running on port ${appConf.port}`)
})
