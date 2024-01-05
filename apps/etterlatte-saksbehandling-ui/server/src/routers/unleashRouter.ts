import express, { Request, Response } from 'express'
import { unleash } from '../utils/unleash'
import { logger } from '../monitoring/logger'
import { sanitize } from '../utils/sanitize'

export const unleashRouter = express.Router()

enum FeatureStatus {
  PAA = 'PAA',
  AV = 'AV',
  UDEFINERT = 'UDEFINERT',
  HENTING_FEILA = 'HENTING_FEILA',
}

unleashRouter.post('/', express.json(), (req: Request, res: Response) => {
  const toggles: string[] = req.body.features

  const isEnabled = (toggle: string): String => {
    if (!unleash) {
      return FeatureStatus.UDEFINERT
    }
    try {
      if (unleash.isEnabled(toggle, undefined)) {
        return FeatureStatus.PAA
      } else {
        return FeatureStatus.AV
      }
    } catch (e) {
      logger.error({
        message: `Fikk feilmelding fra Unleash for toggle ${sanitize(toggle)}, bruker defaultverdi false`,
        stack_trace: JSON.stringify(e),
      })
      return FeatureStatus.HENTING_FEILA
    }
  }

  return res.json(
    toggles.map((toggle) => {
      const enabled = isEnabled(toggle)

      logger.info(`${sanitize(toggle)} enabled: ${enabled}`)

      return {
        toggle: toggle,
        enabled: enabled,
      }
    })
  )
})
