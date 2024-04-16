import express, { Request, Response } from 'express'
import path from 'path'
import { ApiConfig, appConf, ClientConfig } from './config/config'
import { authenticateUser } from './middleware/auth'
import { logger } from './monitoring/logger'
import { tokenMiddleware } from './middleware/getOboToken'
import { proxy } from './middleware/proxy'
import { loggerRouter } from './routers/loggerRouter'
import prometheus from './monitoring/prometheus'
import { norg2Router } from './routers/norg2Router'
import { githubRouter } from './routers/githubRouter'
import { unleashRouter } from './routers/unleashRouter'
import { requestLoggerMiddleware } from './middleware/logging'
import { createProxyMiddleware } from 'http-proxy-middleware'

logger.info(`environment: ${process.env.NODE_ENV}`)

const clientPath = path.resolve(__dirname, '..', 'client')

const app = express()
app.set('trust proxy', 1)

app.use('/', express.static(clientPath))
app.use(requestLoggerMiddleware)

app.use(['/health/isAlive', '/health/isReady'], (_: Request, res: Response) => {
  res.send('OK')
})

app.get('/metrics', async (_: Request, res: Response) => {
  res.set('Content-Type', prometheus.register.contentType)
  res.end(await prometheus.register.metrics())
})

app.use('/api/logg', loggerRouter)
app.use('/api/feature', unleashRouter)

app.get(
  '/internal/selftest/behandling',
  createProxyMiddleware({
    target: `${ApiConfig.behandling.url}/internal/selftest`,
    changeOrigin: true,
  })
)

app.use(authenticateUser) // Alle ruter etter denne er authenticated
app.use('/api/norg2', norg2Router)
app.use('/api/github', githubRouter)

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
    '/api/saksbehandlere',
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

app.use('/api/pdltjenester', tokenMiddleware(ApiConfig.pdltjenester.scope), proxy(ApiConfig.pdltjenester.url))

app.use('/api/beregning', tokenMiddleware(ApiConfig.beregning.scope), proxy(ApiConfig.beregning.url))

app.use('/api/vedtak', tokenMiddleware(ApiConfig.vedtak.scope), proxy(ApiConfig.vedtak.url))

app.use('/api/trygdetid', tokenMiddleware(ApiConfig.trygdetid.scope), proxy(ApiConfig.trygdetid.url))

app.use('/api/trygdetid_v2', tokenMiddleware(ApiConfig.trygdetid.scope), proxy(ApiConfig.trygdetid.url))

app.use(
  ['/api/brev', '/api/dokumenter', '/api/notat'],
  tokenMiddleware(ApiConfig.brev.scope),
  proxy(ApiConfig.brev.url)
)

app.get('/api/config', (_req: Request, res: Response) => res.send(ClientConfig))

app.use(/^(?!.*\/(internal|static)\/).*$/, (_req: Request, res: Response) => {
  return res.sendFile(`${clientPath}/index.html`)
})

app.listen(appConf.port, () => {
  logger.info(`Server running on port ${appConf.port}`)
})
