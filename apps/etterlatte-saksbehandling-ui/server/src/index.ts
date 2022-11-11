import express from 'express'
import cors from 'cors'
import path from 'path'
import { appConf } from './config/config'
import { authenticateUser } from './middleware/auth'
import { healthRouter } from './routers/health'
import { mockRouter } from './routers/mockRouter'
import { modiaRouter } from './routers/modia'
import { expressProxy } from './routers/proxy'
import { logger } from './utils/logger'
import { lokalProxy } from './routers/lokalproxy'
import { requestLogger } from './middleware/logging'

logger.info(`environment: ${process.env.NODE_ENV}`)

const clientPath = path.resolve(__dirname, '../client')
const isDev = process.env.NODE_ENV !== 'production'

const app = express()
app.set('trust proxy', 1)
app.use('/', express.static(clientPath))
app.use(requestLogger(isDev))
app.use(express.json())
app.use('/health', healthRouter)

/** ===============================
 * TODO: Vi må lage et bedre oppsett for håndtering av mock og auth APIer...
 =============================== */
if (isDev) {
  app.use(cors({ origin: 'http://localhost:3000' }))

  app.use('/api/modiacontextholder/', modiaRouter) // bytte ut med etterlatte-innlogget?
  if (!!process.env.VILKAARSVURDERING_API_URL) {
    app.use(authenticateUser) // Alle ruter etter denne er authenticated
    app.use(
      '/api/vilkaarsvurdering',
      expressProxy(
        process.env.VILKAARSVURDERING_API_URL!!,
        'api://dev.etterlatte.etterlatte-vilkaarsvurdering-api/.default'
      )
    )
  }

  if (!!process.env.BREV_API_URL) {
    app.use(authenticateUser) // Alle ruter etter denne er authenticated
    app.use('/brev', expressProxy(process.env.BREV_API_URL!!, 'api://dev.etterlatte.etterlatte-brev-api/.default'))
  }

  if (!!process.env.API_URL) {
    app.use(authenticateUser) // Alle ruter etter denne er authenticated
    app.use('/api', expressProxy(`${process.env.API_URL}`, 'api://dev.etterlatte.etterlatte-api/.default'))
  } else {
    app.use('/api', mockRouter)
  }
} else {
  app.use(authenticateUser) // Alle ruter etter denne er authenticated
  app.use(
    '/api/vilkaarsvurdering',
    expressProxy(`${process.env.VILKAARSVURDERING_API_URL}`, 'api://f4cf400f-8ef9-406f-baf1-8218f8f7edac/.default')
  )
  app.use('/api/modiacontextholder/', modiaRouter) // bytte ut med etterlatte-innlogget?
  app.use(
    '/api/behandling/:behandlingsid/kommerbarnettilgode',
    expressProxy(`${process.env.BEHANDLING_API_URL}`, 'api://59967ac8-009c-492e-a618-e5a0f6b3e4e4/.default')
  )

  app.use('/api', expressProxy(`${process.env.API_URL}`, 'api://783cea60-43b5-459c-bdd3-de3325bd716a/.default'))
  app.use('/brev', expressProxy(`${process.env.BREV_API_URL}`, 'api://d6add52a-5807-49cd-a181-76908efee836/.default'))
}

app.use(/^(?!.*\/(internal|static)\/).*$/, (req: any, res: any) => {
  return res.sendFile(`${clientPath}/index.html`)
})

app.listen(appConf.port, () => {
  logger.info(`Server running on port ${appConf.port}`)
})
