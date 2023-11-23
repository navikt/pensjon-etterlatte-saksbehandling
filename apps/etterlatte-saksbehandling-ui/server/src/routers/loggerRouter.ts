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

function findAndSanitizeUrl(url?: String): String {
  if (url) {
    const splittedUrl = url.split('/')
    splittedUrl.map((urlpart) => {
      if (GYLDIG_FNR(urlpart)) {
        return urlpart.substring(0, 5)
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
    if (body.stackInfo) {
      if (stackInfoIsInvalid(body.stackInfo)) {
        frontendLogger.error('Cannot parse stackInfo, body: \n', JSON.stringify(body), '\n url: ', maybeUrl)
      } else {
        sourceMapMapper(body.stackInfo)
          .then((position) => {
            const message = body.stackInfo.message
            const error = JSON.stringify(body.stackInfo.error)
            const component = `'${position.source}' (line: ${position.line}, col: ${position.column})`

            frontendLogger.error({
              message: message || 'Request error on: ',
              stack_trace: `Error occurred in ${component}:\n${message}\n${error}`,
              user_device: JSON.stringify(body.jsonContent.userDeviceInfo),
              user_agent: body.jsonContent.userAgent,
              url: maybeUrl,
            })
          })
          .catch((err) => {
            frontendLogger.error('Request got error on: \n', err, '\n url: ', maybeUrl)
          })
      }
    } else {
      frontendLogger.error(
        `General error from frontend: ${JSON.stringify(body.data)} \n details: ${JSON.stringify(
          body.jsonContent
        )} url: ${maybeUrl}`
      )
    }
  }
  res.sendStatus(200)
})
