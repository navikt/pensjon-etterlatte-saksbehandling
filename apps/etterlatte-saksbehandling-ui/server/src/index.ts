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
import brev from './routers/brev'
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

if (isDev) {
  app.use(cors({ origin: 'http://localhost:3000' }))
  if (process.env.VILKAARSVURDERING_DEV) {
    app.use('/api/vilkaarsvurdering', lokalProxy('http://localhost:8087/api/vilkaarsvurdering'))
  }
  if (process.env.BREV_DEV) {
    app.use('/brev', brev)
  }
  app.use('/api', mockRouter)
} else {
  app.use(authenticateUser) // Alle ruter etter denne er authenticated
  app.use(
    '/api/vilkaarsvurdering',
    expressProxy(`${process.env.VILKAARSVURDERING_API_URL}`, 'api://f4cf400f-8ef9-406f-baf1-8218f8f7edac/.default')
  )
  app.use('/api', expressProxy(`${process.env.API_URL}`, 'api://783cea60-43b5-459c-bdd3-de3325bd716a/.default'))
  app.use('/brev', expressProxy(`${process.env.BREV_API_URL}`, 'api://d6add52a-5807-49cd-a181-76908efee836/.default'))
}

app.use('/modiacontextholder/api/', modiaRouter) // bytte ut med etterlatte-innlogget?
app.use(/^(?!.*\/(internal|static)\/).*$/, (req: any, res: any) => {
  return res.sendFile(`${clientPath}/index.html`)
})

app.listen(appConf.port, () => {
  logger.info(`Server running on port ${appConf.port}`)
})
