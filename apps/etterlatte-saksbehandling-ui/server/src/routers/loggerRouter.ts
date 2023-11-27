import express from 'express'
import { frontendLogger } from '../monitoring/logger'
import sourceMap, { NullableMappedPosition } from 'source-map'
import * as fs from 'fs'

export const loggerRouter = express.Router()

export interface IStackLineNoColumnNo {
  lineno: number
  columnno: number
  message: any
  error: any
}

function stackInfoIsInvalid(numbers: IStackLineNoColumnNo): boolean {
  return numbers.lineno < 1 || numbers.columnno < 0
}

const sourcemapLocation = '/app/client/assets'
async function sourceMapMapper(numbers: IStackLineNoColumnNo): Promise<NullableMappedPosition> {
  const sourcemapFile = fs.readdirSync(sourcemapLocation).find((file) => file.includes('.map')) ?? ''
  const rawSourceMap = fs.readFileSync(`${sourcemapLocation}/${sourcemapFile}`).toString()
  const smc = await new sourceMap.SourceMapConsumer(rawSourceMap)
  return Promise.resolve(smc.originalPositionFor({ line: numbers.lineno, column: numbers.columnno }))
}

const GYLDIG_FNR = (input: string | undefined) => /^\d{11}$/.test(input ?? '')

function findAndSanitizeUrl(url?: string): string {
  if (url) {
    const splittedUrl = url.split('/')
    splittedUrl.map((urlpart) => {
      if (GYLDIG_FNR(urlpart)) {
        return urlpart.substring(0, 5).concat('******')
      }
      return urlpart
    })
  }
  return ''
}

loggerRouter.post('/', express.json(), (req, res) => {
  const body = req.body
  if (!process.env.NAIS_CLUSTER_NAME) {
    frontendLogger.info(`Nais cluster unavailable: ${JSON.stringify(body)}`)
  } else if (body.type && body.type === 'info') {
    frontendLogger.info('Frontendlogging: ', JSON.stringify(body))
  } else {
    const maybeUrl = findAndSanitizeUrl(body.jsonContent.url)
    const errorObject = {
      request_uri: maybeUrl,
      user_device: JSON.stringify(body.jsonContent.userDeviceInfo),
      user_agent: body.jsonContent.userAgent,
    }
    if (body.stackInfo) {
      if (stackInfoIsInvalid(body.stackInfo)) {
        frontendLogger.error({
          ...errorObject,
          message: 'Cannot parse stackInfo',
          stack_trace: JSON.stringify(body),
        })
      } else {
        sourceMapMapper(body.stackInfo)
          .then((position) => {
            const message = body.stackInfo.message
            const error = JSON.stringify(body.stackInfo.error)
            const component = `'${position.source}' (line: ${position.line}, col: ${position.column})`

            frontendLogger.error({
              ...errorObject,
              message: message || 'Request error on: ',
              stack_trace: `Error occurred in ${component}:\n${message}\n${error}`,
            })
          })
          .catch((err) => {
            frontendLogger.error({
              ...errorObject,
              message: `Got error on request`,
              stack_trace: JSON.stringify({ ...err, ...body }),
            })
          })
      }
    } else {
      frontendLogger.error({
        ...errorObject,
        message: `General error from frontend. ${body.data.msg ?? ''}`,
        stack_trace: JSON.stringify(body),
      })
    }
  }
  res.sendStatus(200)
})
