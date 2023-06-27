import { createProxyMiddleware } from 'http-proxy-middleware'
import { ClientRequest } from 'http'
import { Request, Response } from 'express'
import { logger } from '../monitoring/logger'

export const proxy = (host: string) =>
  createProxyMiddleware({
    target: host,
    changeOrigin: true,
    onProxyReq: (proxyReq: ClientRequest, req: Request, res: Response) => {
      logger.info(`proxying ${host}`)
      proxyReq.setHeader('Authorization', `Bearer ${res.locals.token}`)
    },
  })
