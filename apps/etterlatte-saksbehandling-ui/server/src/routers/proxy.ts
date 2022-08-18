import proxy from 'express-http-proxy'
import { getOboToken } from '../middleware/getOboToken'
import { logger } from '../utils/logger'

const options: any = (scope: string) => ({
  parseReqBody: true,
  proxyReqOptDecorator: async (options: any, req: any) => {
    const oboToken = await getOboToken(req.headers.authorization, scope)
    options.headers.Authorization = `Bearer ${oboToken}`
    return options
  },
  proxyReqPathResolver: (req: any) => {
    return req.originalUrl.replace(`https://etterlatte-saksbehandling.dev.intern.nav.no`, '')
  },
  proxyErrorHandler: (err: any, res: any, next: any) => {
    logger.error('Proxy error: ', err)
    next(err)
  },
})

export const expressProxy = (host: string, scope: string) => proxy(host, options(scope))
