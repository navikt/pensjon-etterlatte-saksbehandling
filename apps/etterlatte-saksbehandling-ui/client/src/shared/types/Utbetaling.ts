export interface SimulertBeregning {
  gjelderId: string
  datoBeregnet: string
  infomelding: string | null
  beloep: number
  kommendeUtbetalinger: SimulertBeregningsperiode[]
  etterbetaling: UtbetalingAggregert[]
  tilbakekreving: UtbetalingAggregert[]
}

export interface SimulertBeregningsperiode {
  fom: string
  tom: string | undefined
  gjelderId: string
  forfall: string
  utbetalesTilId: string
  feilkonto: boolean
  enhet: string
  konto: string
  behandlingskode: string
  beloep: number
  tilbakefoering: boolean
  klassekode: string
  klassekodeBeskrivelse: string
  klasseType: string
}

export interface UtbetalingAggregert {
  klassekode: string
  klassekodeBeskrivelse: string
  klasseType: string
  beloep: number
}
