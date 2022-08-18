import express from 'express'
import path from 'path'
import { appConf } from './config/config'
import { authenticateUser } from './middleware/auth'
import { healthRouter } from './routers/health'
import { mockRouter } from './routers/mockRouter'
import { modiaRouter } from './routers/modia'
import { expressProxy } from './routers/proxy'
import { logger } from './utils/logger'
import brev from './routers/brev'

const app = express()

const clientPath = path.resolve(__dirname, '../client')
const isDev = process.env.NODE_ENV !== 'production'

logger.info(`environment: ${process.env.NODE_ENV}`)

app.set('trust proxy', 1)

app.use('/', express.static(clientPath))

// requestlogger-middleware
app.use((req, res, next) => {
  logger.info('Request', { ...req.headers, url: req.url })
  next()
})

app.use(express.json())

// cors-middleware
app.use(function (req, res, next) {
  res.header('Access-Control-Allow-Origin', 'http://localhost:3000') //Todo: fikse domene
  res.header('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE')
  res.header('Access-Control-Allow-Headers', 'Content-Type')
  res.header('Access-Control-Allow-Credentials', 'true')
  next()
})

app.use('/health', healthRouter) // åpen rute
if (process.env.DEVELOPMENT !== 'true') {
  app.use(authenticateUser) // Alle ruter etter denne er authenticated
}

app.use('/modiacontextholder/api/', modiaRouter) // bytte ut med etterlatte-innlogget?

if (isDev) {
  app.use('/api', mockRouter)

  if (process.env.BREV_DEV) {
    app.use('/brev', brev)
  }
} else {
  logger.info('Proxy-kall')
  app.use('/api', expressProxy(`${process.env.API_URL}`, 'api://783cea60-43b5-459c-bdd3-de3325bd716a/.default'))
  app.use('/brev', expressProxy(`${process.env.BREV_API_URL}`, 'api://d6add52a-5807-49cd-a181-76908efee836/.default'))
}

app.use(/^(?!.*\(internal|static)\).*$/, (req: any, res: any) => {
  return res.sendFile(`${clientPath}/index.html`)
})

app.listen(appConf.port, () => {
  logger.info(`Server running on port ${appConf.port}`)
})
