import express from 'express'
import { ApiConfig } from '../config/config'
import { logger } from '../monitoring/logger'

export const selftestRouter = express.Router()

selftestRouter.get('/', express.json(), async (_, res) => {
  const results: Promise<IPingResult>[] = Object.entries(ApiConfig).map(async ([serviceName, urlscope]) => {
    // Siden utbetaling kjører R&R med restmodul fungerer vårt interne health/isready dårlig (den blir aldri ready)
    // og vi går derfor i stedet mot standard isready fra R&R
    const isReadyEndpoint = serviceName === 'utbetaling' ? `${urlscope.url}/isready` : `${urlscope.url}/health/isready`

    const statuscode = await fetch(isReadyEndpoint)
      .then((res) => res.status)
      .catch((err) => {
        logger.warn(`${serviceName} is down.`, err)
        return 500
      })
    return {
      serviceName: serviceName,
      result: statuscode === 200 ? ServiceStatus.UP : ServiceStatus.DOWN,
      endpoint: urlscope.url,
      description: serviceName,
    }
  })
  Promise.all(results)
    .then((e) =>
      e.map((pingresult) => {
        return pingresult
      })
    )
    .then((allchecks) => {
      const aggregateResult = allchecks.some((res) => res.result == ServiceStatus.DOWN) ? 1 : 0
      const selfTestReport = {
        application: 'etterlatte-saksbehandling',
        timestamp: new Date().toISOString(),
        aggregateResult: aggregateResult,
        checks: allchecks,
      }

      res.json(selfTestReport)
    })
    .catch((err) => res.status(500).send(err))
    .finally(() => {
      return
    })
})

interface IPingResult {
  serviceName: string
  result: ServiceStatus
  endpoint: string
  description: string
}

enum ServiceStatus {
  UP = 0,
  DOWN = 1,
}
