import winston, { format } from 'winston'
const { colorize, combine, timestamp, simple, json } = format

export const logger = winston.createLogger({
  level: 'info',
  format: winston.format.json(),
  defaultMeta: { service: 'etterlatte-saksbehandling-ui' },
})

logger.add(
  new winston.transports.Console({
    format: winston.format.simple(),
  })
)

const production = combine(timestamp(), json())
const dev = combine(colorize(), simple())

export const frontendLogger = winston.createLogger({
  level: 'info',
  format: process.env.NAIS_CLUSTER_NAME ? production : dev,
  defaultMeta: {
    service: 'etterlatte-saksbehandling-ui-client',
  },
})

frontendLogger.add(
  new winston.transports.Console({
    format: winston.format.simple(),
  })
)
