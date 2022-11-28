import { NextFunction, Request, Response } from 'express'
import { logger } from '../utils/logger'

const requestLoggerIncludeHeaders = (req: Request, res: Response, next: NextFunction) => {
  res.on('finish', () => {
    logger.info('Response code', { statusCode: res.statusCode })
  })
  logger.info('Request', { ...req.headers, url: req.url })
  next()
}

const requestLoggerExcludeHeaders = (req: Request, res: Response, next: NextFunction) => {
  res.on('finish', () => {
    logger.info('Response code', { statusCode: res.statusCode })
  })
  logger.info('Request', { url: req.url })
  next()
}

export const requestLogger = (includeHeaders: boolean) =>
  includeHeaders ? requestLoggerIncludeHeaders : requestLoggerExcludeHeaders
