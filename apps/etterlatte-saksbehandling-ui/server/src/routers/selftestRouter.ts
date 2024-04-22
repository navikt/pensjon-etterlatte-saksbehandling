import express from 'express'
import { createProxyMiddleware } from 'http-proxy-middleware'
import { ApiConfig } from '../config/config'
import { logger } from '../monitoring/logger'

export const selftestRouter = express.Router()

selftestRouter.get('internal/selftest', express.json(), () => {
  Object.entries(ApiConfig).forEach((e) => {
    createUnauthProxySelftestFor(e[1].url, e[0])
  })
})

function createUnauthProxySelftestFor(targetUrl: string, servicename: string) {
  createProxyMiddleware({
    target: targetUrl,
    pathRewrite: {
      '^/internal/selftest': '/health/isready',
    },
    changeOrigin: false,
    on: {
      proxyReq: (proxyReq, req) => {
        logger.info(`proxying isready for service ${servicename} ${req.url}`)
      },
      proxyRes: (proxyRes, req, res) => {
        logger.info(`res from ${servicename} isready: ${res.req.statusCode}`)
      },
      error: (err) => {
        logger.error(`Error from ${servicename} - isready`, err)
      },
    },
  })
}
