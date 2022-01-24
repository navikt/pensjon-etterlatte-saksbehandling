import winston from "winston";

export const logger = winston.createLogger({
    level: "info",
    format: winston.format.json(),
    defaultMeta: { service: "etterlatte-saksbehandling-ui" }
});


//if (process.env.NODE_ENV !== 'production') {
  logger.add(new winston.transports.Console({
    format: winston.format.simple(),
  }));
//}