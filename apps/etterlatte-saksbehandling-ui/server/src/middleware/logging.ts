import { NextFunction, Request, Response } from 'express'
import { logger } from '../monitoring/logger'
import { sanitizeUrl } from '../utils/sanitize'

const kanLoggeRequest = (input: string | undefined) => !/(health|metrics)/.test(input ?? '')

export const requestLoggerMiddleware = (req: Request, _: Response, next: NextFunction) => {
  if (kanLoggeRequest(req.url)) {
    logger.info(`${req.method} ${sanitizeUrl(req.url)}`)
  }

  next()
}
