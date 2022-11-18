import express, { Request, Response } from 'express'
import path from 'path'
import { appConf, ApiConfig } from './config/config'
import { authenticateUser } from './middleware/auth'
import { mockRouter } from './routers/mockRouter'
import { modiaRouter } from './routers/modia'
import { frontendLogger, logger } from './utils/logger'
import { requestLogger } from './middleware/logging'
import { tokenMiddleware } from './middleware/getOboToken'
import { proxy } from './middleware/proxy'

logger.info(`environment: ${process.env.NODE_ENV}`)

const clientPath = path.resolve(__dirname, '../client')
const isDev = process.env.NODE_ENV !== 'production'

const app = express()
app.set('trust proxy', 1)
app.use('/', express.static(clientPath))
app.use(requestLogger(isDev))

app.use(['/health/isAlive', '/health/isReady'], (req: Request, res: Response) => {
  res.send('OK')
})

if (isDev) {
  // TODO: Legge til resterende mocks
  logger.info('Mocking all endpoints')
  app.use('/api', mockRouter)
} else {
  app.post('/api/logg', express.json(), (req, res) => {
    let body = req.body
    if (body.type && body.type === 'info') {
      frontendLogger.info('Request', req.body)
    } else {
      frontendLogger.error('Request', req.body)
    }
    res.sendStatus(200)
  })

  app.use(authenticateUser) // Alle ruter etter denne er authenticated
  app.use('/api/modiacontextholder/', modiaRouter) // bytte ut med etterlatte-innlogget?

  app.use(
    '/api/vilkaarsvurdering',
    tokenMiddleware(ApiConfig.vilkaarsvurdering.scope),
    proxy(ApiConfig.vilkaarsvurdering.url!!)
  )

  app.use(
    '/api/behandling/:behandlingsid/kommerbarnettilgode',
    tokenMiddleware(ApiConfig.behandling.scope),
    proxy(ApiConfig.behandling.url!!)
  )

  app.use('/api/beregning', tokenMiddleware(ApiConfig.beregning.scope), proxy(ApiConfig.beregning.url!!))

  app.use('/api/vedtak', tokenMiddleware(ApiConfig.vedtak.scope), proxy(ApiConfig.vedtak.url!!))

  app.use(['/api/brev', '/api/dokumenter'], tokenMiddleware(ApiConfig.brev.scope), proxy(ApiConfig.brev.url!!))

  app.use('/api', tokenMiddleware(ApiConfig.api.scope), proxy(ApiConfig.api.url!!))
}

// Body parser må komme etter proxy middleware
app.use(express.json())

app.use(/^(?!.*\/(internal|static)\/).*$/, (req: Request, res: Response) => {
  return res.sendFile(`${clientPath}/index.html`)
})

app.listen(appConf.port, () => {
  logger.info(`Server running on port ${appConf.port}`)
})
