import { Context, Strategy, getFeatureToggleDefinitions, initialize } from 'unleash-client'
import { FeatureToggleConfig } from '../config/config'
import GradualRolloutRandomStrategy from 'unleash-client/lib/strategy/gradual-rollout-random'
import { logger } from './logger'

export const unleashContext = {
  cluster: FeatureToggleConfig.cluster,
}

class ByClusterStrategy extends Strategy {
  constructor() {
    super('byCluster')
  }

  // eslint-disable-next-line no-unused-vars
  isEnabled(parameters: any, context: Context) {
    return parameters['cluster'].includes(FeatureToggleConfig.cluster)
  }
}

export const unleash = initialize({
  url: FeatureToggleConfig.uri,
  appName: FeatureToggleConfig.applicationName,
  strategies: [new ByClusterStrategy(), new GradualRolloutRandomStrategy()],
})

unleash.on('synchronized', () => {
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

unleash.on('error', (err: Error) => {
  logger.error(err)
})

unleash.on('warn', (msg: string) => {
  logger.error(msg)
})
