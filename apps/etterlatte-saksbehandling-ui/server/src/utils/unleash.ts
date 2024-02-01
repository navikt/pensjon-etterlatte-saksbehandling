import { destroy, getFeatureToggleDefinitions, initialize } from 'unleash-client'
import { FeatureToggleConfig } from '../config/config'
import GradualRolloutRandomStrategy from 'unleash-client/lib/strategy/gradual-rollout-random'
import { logger } from '../monitoring/logger'
import GradualRolloutUserIdStrategy from 'unleash-client/lib/strategy/gradual-rollout-user-id'

export const unleash =
  FeatureToggleConfig.host !== ''
    ? initialize({
        url: FeatureToggleConfig.host + '/api',
        customHeaders: {
          Authorization: FeatureToggleConfig.token,
        },
        appName: FeatureToggleConfig.applicationName,

        strategies: [new GradualRolloutRandomStrategy(), new GradualRolloutUserIdStrategy()],
      })
    : null

unleash?.on('synchronized', () => {
  logger.info(`Unleash synchronized`)

  const definitions = getFeatureToggleDefinitions()

  definitions?.map((definition) => {
    if (definition.name.includes('etterlatte')) {
      logger.info(`Toggle ${definition.name} is enabled: ${definition.enabled}`)
      logger.info('Strategies:')

      definition.strategies.map((strat) => {
        logger.info(strat.name, { ...strat.parameters })
      })
    }
  })
})

unleash?.on('error', (err: Error) => {
  logger.error({
    message: err.message || 'Feil oppsto i unleash: ',
    stack_trace: err.stack,
  })
})

unleash?.on('warn', (msg: string) => {
  logger.error(msg)
})

process.on('SIGTERM', () => {
  logger.info('App is shutting down – destroying unleash client')
  destroy()
})
