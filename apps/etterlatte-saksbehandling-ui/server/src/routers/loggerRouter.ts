import express from 'express'
import { frontendLogger, logger } from '../monitoring/logger'
import sourceMap, { NullableMappedPosition } from 'source-map'
import * as fs from 'fs'
import { parseJwt } from '../utils/parsejwt'
import { sanitizeUrl } from '../utils/sanitize'
/* eslint @typescript-eslint/no-explicit-any: 0 */ // --> OFF

export const loggerRouter = express.Router()

export interface IStackLineNoColumnNo {
  lineno: number
  columnno: number
  message: any
  error: any
}

function isStackInfoValid(numbers: IStackLineNoColumnNo): boolean {
  return numbers.lineno > 0 && numbers.columnno >= 0
}

const sourcemapLocation = '/app/client/assets'

async function sourceMapMapper(numbers: IStackLineNoColumnNo): Promise<NullableMappedPosition> {
  const sourcemapFile = fs.readdirSync(sourcemapLocation).find((file) => file.includes('.map')) ?? ''
  const rawSourceMap = fs.readFileSync(`${sourcemapLocation}/${sourcemapFile}`).toString()
  const smc = await new sourceMap.SourceMapConsumer(rawSourceMap)
  return Promise.resolve(smc.originalPositionFor({ line: numbers.lineno, column: numbers.columnno }))
}

export function getNAVident(authorizationHeader: string | undefined): string | undefined {
  if (!authorizationHeader) return
  const bearerToken = authorizationHeader.split(' ')[1]
  const parsedToken = parseJwt(bearerToken)
  return parsedToken.NAVident
}

loggerRouter.post('/', express.json(), (req, res): void => {
  const logEvent = req.body as LogEvent
  const user = getNAVident(req.headers.authorization)
  const errorData = logEvent.data

  if (logEvent.type && logEvent.type.toLowerCase() === 'info') {
    frontendLogger.info('Frontendlogging: ', JSON.stringify(logEvent))
  } else if (logEvent.type && logEvent.type.toLowerCase() === 'warning') {
    frontendLogger.info('Frontendlogging: ', JSON.stringify(logEvent))
  } else {
    if (logEvent.stackInfo && isStackInfoValid(logEvent.stackInfo)) {
      sourceMapMapper(logEvent.stackInfo!)
        .then((position) => {
          const message = logEvent.stackInfo?.message
          const stackInfoError = JSON.stringify(logEvent.stackInfo?.error)
          const component = `'${position.source}' (line: ${position.line}, col: ${position.column})`

          frontendLogger.error({
            message: message || 'Feil ved request',
            stack_trace: `Error occurred in ${component}:\n${message}\n${stackInfoError}`,
            ...mapCommonFields(user, logEvent.jsonContent, errorData),
          })
        })
        .catch((err) => {
          logger.error(err)

          frontendLogger.error({
            message: logEvent.stackInfo?.message || errorData?.msg || 'Ukjent feil oppsto (sourceMapMapper)',
            stack_trace: errorData?.errorInfo ? JSON.stringify(errorData?.errorInfo) : JSON.stringify(logEvent),
            ...mapCommonFields(user, logEvent.jsonContent, errorData),
          })
        })
    } else {
      frontendLogger.error({
        message: errorData?.msg || 'Ukjent feil oppsto',
        stack_trace: errorData?.errorInfo ? JSON.stringify(errorData?.errorInfo) : JSON.stringify(logEvent),
        ...mapCommonFields(user, logEvent.jsonContent, errorData),
      })
    }
  }
  res.sendStatus(200)
})

// Er vel litt optimistisk
const mapCommonFields = (user?: string, jsonContent?: JsonContent, errorData?: ErrorData) => {
  return {
    user,
    request_uri: sanitizeUrl(jsonContent?.url),
    outbound_uri: errorData?.apiErrorInfo?.url,
    method: errorData?.apiErrorInfo?.method,
    user_agent: jsonContent?.userAgent,
  }
}

interface LogEvent {
  type: string
  data?: ErrorData
  stackInfo?: IStackLineNoColumnNo
  jsonContent?: JsonContent
}

interface ErrorData {
  msg: string
  errorInfo?: ErrorInfo
  apiErrorInfo?: ApiErrorInfo
  err?: Error
}

interface ErrorInfo {
  componentStack?: string | null
  digest?: string | null
}

interface ApiErrorInfo {
  url: string
  method: string
  error?: JsonError
}

interface JsonError {
  status: number
  detail: string
  code?: string
  meta?: Record<string, unknown>
}

interface JsonContent {
  url: string
  userAgent: string
}
