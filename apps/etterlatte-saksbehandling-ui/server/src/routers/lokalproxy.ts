import proxy from 'express-http-proxy'
import { logger } from '../utils/logger'
import fetch from 'node-fetch'

async function getTokenFromMockServer() {
  const body: any = {
    client_id: 'clientId',
    client_secret: 'not_so_secret',
    scope: 'clientId',
    grant_type: 'client_credentials',
  }

  const response = await fetch('http://localhost:8082/azure/token', {
    method: 'post',
    body: Object.keys(body)
      .map((key) => encodeURIComponent(key) + '=' + encodeURIComponent(body[key]))
      .join('&'),
    headers: {
      'content-type': 'application/x-www-form-urlencoded',
    },
  })
  const json = await response.json()
  return json.access_token
}

export const lokalProxy = (host: string) =>
  proxy(host, {
    parseReqBody: true,

    proxyReqOptDecorator: async (options: any) => {
      options.headers.Authorization = `Bearer ${await getTokenFromMockServer()}`
      return options
    },
    proxyReqPathResolver: (req: any) => {
      return req.originalUrl
    },
    proxyErrorHandler: (err: any, res: any, next: any) => {
      logger.error('Proxy error: ', err)
      next(err)
    },
  })
