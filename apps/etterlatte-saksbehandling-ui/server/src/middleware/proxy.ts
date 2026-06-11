import { createProxyMiddleware } from 'http-proxy-middleware'
import { ClientRequest } from 'http'
import { NextFunction, Request, Response } from 'express'
import { logger } from '../monitoring/logger'
import { randomUUID } from 'crypto'

export const proxy = (host: string) => {
  const middleware = createProxyMiddleware({
    target: host,
    changeOrigin: true,
    on: {
      proxyReq: (proxyReq: ClientRequest, req: Request, res: Response) => {
        logger.info(`proxying ${host}`)
        proxyReq.setHeader('Authorization', `Bearer ${res.locals.token}`)
        proxyReq.setHeader('X-Correlation-ID', randomUUID())
      },
    },
  })
  // http-proxy-middleware v4 bruker req.url for å bestemme stien som sendes til backend.
  // Express stripper path-prefikset fra req.url når app.use(['/api/foo', ...], ...) matcher,
  // så vi må gjenopprette full sti fra req.originalUrl før proxyen kjøres.
  return (req: Request, res: Response, next: NextFunction) => {
    req.url = req.originalUrl
    middleware(req, res, next)
  }
}
