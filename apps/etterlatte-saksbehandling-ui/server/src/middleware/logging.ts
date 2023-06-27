import { NextFunction, Request, Response } from 'express'
import { logger } from '../monitoring/logger'

const requestLoggerIncludeHeaders = (req: Request, res: Response, next: NextFunction) => {
  if (!req.url.includes('health')) {
    logger.info('Request', { ...req.headers, url: req.url })
  }
  next()
}

const requestLoggerExcludeHeaders = (req: Request, res: Response, next: NextFunction) => {
  if (!req.url.includes('health')) {
    logger.info('Request', { url: req.url })
  }
  next()
}

export const requestLogger = (includeHeaders: boolean) =>
  includeHeaders ? requestLoggerIncludeHeaders : requestLoggerExcludeHeaders
