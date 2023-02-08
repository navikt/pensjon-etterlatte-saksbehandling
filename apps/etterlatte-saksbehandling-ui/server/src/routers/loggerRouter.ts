import express from 'express'
import { frontendLogger } from '../utils/logger'
import sourceMap, { NullableMappedPosition } from 'source-map'
import * as fs from 'fs'

export const loggerRouter = express.Router()

export interface IStackLineNoColumnNo {
  lineno: number
  columnno: number
  error: any
}

loggerRouter.post('/', express.json(), (req, res) => {
  const body = req.body
  if (!process.env.NAIS_CLUSTER_NAME) {
    frontendLogger.info('Nais cluster unavailable')
  } else if (body.type && body.type === 'info') {
    frontendLogger.info('Frontendlogging: ', body)
  } else {
    if (body.stackInfo) {
      sourceMapMapper(body.stackInfo)
        .then((mappedPositionData) => {
          frontendLogger.error(
            `Request got error on: \n ${JSON.stringify(mappedPositionData)} \n error: ${JSON.stringify(
              body.stackInfo.error
            )} \n details: ${JSON.stringify(body.jsonContent)}`
          )
        })
        .catch((err) => {
          frontendLogger.error('Request got error on: \n', err)
        })
    } else {
      frontendLogger.error(
        `General error from frontend: ${JSON.stringify(body.data)} \n details: ${JSON.stringify(body.jsonContent)}`
      )
    }
  }
  res.sendStatus(200)
})

const sourcemapLocation = '/app/client/assets'
async function sourceMapMapper(numbers: IStackLineNoColumnNo): Promise<NullableMappedPosition> {
  const sourcemapFile = fs.readdirSync(sourcemapLocation).find((file) => file.includes('.map')) ?? ''
  const rawSourceMap = fs.readFileSync(`${sourcemapLocation}/${sourcemapFile}`).toString()
  const smc = await new sourceMap.SourceMapConsumer(rawSourceMap)
  return Promise.resolve(smc.originalPositionFor({ line: numbers.lineno, column: numbers.columnno }))
}
