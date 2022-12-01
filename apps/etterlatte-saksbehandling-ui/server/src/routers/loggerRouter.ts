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
  let body = req.body
  if (body.type && body.type === 'info') {
    frontendLogger.info('Frontendlogging: ', body)
  } else {
    if (body.stackInfo) {
      sourceMapMapper(body.stackInfo)
        .then((data) => {
          frontendLogger.error(
            `Request got error on: \n ${data} \n error: ${body.stackInfo.error} \n details: ${body.jsonContent}`
          )
        })
        .catch((err) => {
          frontendLogger.error('Request got error on: \n', err)
        })
    }
  }
  res.sendStatus(200)
})

const sourcemapLocation = '/app/client/assets'
async function sourceMapMapper(numbers: IStackLineNoColumnNo): Promise<NullableMappedPosition> {
  let sourcemapFile = ''
  fs.readdirSync(sourcemapLocation).forEach((file) => {
    if (file.includes('.map')) {
      sourcemapFile = file
    }
  })
  const rawSourceMap = fs.readFileSync(`${sourcemapLocation}/${sourcemapFile}`).toString()
  const smc = await new sourceMap.SourceMapConsumer(rawSourceMap)
  return Promise.resolve(smc.originalPositionFor({ line: numbers.lineno, column: numbers.columnno }))
}
