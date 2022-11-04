import { NextFunction, Request, Response } from 'express'
import { logger } from '../utils/logger'

const requestLoggerIncludeHeaders = (req: Request, res: Response, next: NextFunction) => {
  logger.info('Request', { ...req.headers, url: req.url })
  next()
}

const requestLoggerExcludeHeaders = (req: Request, res: Response, next: NextFunction) => {
  logger.info('Request', { url: req.url })
  next()
}

export const requestLogger = (includeHeaders: boolean) =>
  includeHeaders ? requestLoggerIncludeHeaders : requestLoggerExcludeHeaders
