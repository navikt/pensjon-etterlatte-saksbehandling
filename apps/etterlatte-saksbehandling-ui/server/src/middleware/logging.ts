import { NextFunction, Request, Response } from 'express'
import { logger } from '../monitoring/logger'
import { sanitize, sanitizeUrl } from '../utils/sanitize'

export const kanLoggeRequest = (input: string | undefined) => !/(health|metrics)/.test(input ?? '')

export const requestLoggerMiddleware = (req: Request, res: Response, next: NextFunction) => {
  if (kanLoggeRequest(req.url)) {
    res.locals.start = Date.now()
    res.on('finish', function () {
      const response_time = Date.now() - res.locals.start
      if (kanLoggeRequest(req.url)) {
        logger.info({
          message: `${sanitize(req.method)} ${sanitizeUrl(req.url)} ms: ${response_time}`,
          ...(response_time && { x_response_time: response_time }),
        })
      }
    })
  }

  next()
}
