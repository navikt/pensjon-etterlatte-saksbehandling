import { Context, Strategy, initialize } from 'unleash-client'
import { FeatureToggleConfig } from '../config/config'
import GradualRolloutRandomStrategy from 'unleash-client/lib/strategy/gradual-rollout-random'
import { logger } from '../monitoring/logger'

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

// Some useful life-cycle events
unleash.on('ready', logger.info)
unleash.on('synchronized', logger.info)
unleash.on('registered', logger.info)
unleash.on('changed', () => {
  logger.info('Feature toggles har blitt oppdatert')
})
unleash.on('error', (err: Error) => {
  logger.error({
    message: err.message || 'Feil oppsto i unleash: ',
    stack_trace: err.stack,
  })
})

unleash.on('warn', (msg: string) => {
  logger.error(msg)
})
