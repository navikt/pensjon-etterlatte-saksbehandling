import Transport from 'winston-transport'
import { Counter } from 'prom-client'
import prometheus from './prometheus'

// noinspection JSAnnotator
export class PrometheusTransport extends Transport {
  private readonly register: any = null
  private counter: any = null

  constructor() {
    super()

    this.register = prometheus.register
    this.counter = new Counter({
      name: 'winston_events_total',
      help: 'All log entries passed to winston, labelled with log level.',
      labelNames: ['level'],
      registers: [this.register],
    })
  }

  log(info: any, callback: any) {
    setImmediate(() => {
      this.emit('logged', info)

      this.counter.inc({ level: info.level })

      callback()
    })
  }
}
