import winston from 'winston'

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
