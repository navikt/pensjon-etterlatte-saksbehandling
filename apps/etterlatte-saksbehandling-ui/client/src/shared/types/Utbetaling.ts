export interface SimulertBeregning {
  gjelderId: string
  datoBeregnet: string
  infomelding: string | null
  beloep: number
  kommendeUtbetalinger: SimulertBeregningsperiode[]
  etterbetaling: SimulertBeregningsperiode[]
  tilbakekreving: SimulertBeregningsperiode[]
  oppsummeringer: SimulertBeregningOppsummeringer
}

export interface SimulertBeregningOppsummeringer {
  perAar: SimulertBeregningPerAar[]
  forPerioden: SimulertBeregningOppsummering
}

export interface SimulertBeregningPerAar {
  aarstall: number
  oppsummering: SimulertBeregningOppsummering
}

export interface SimulertBeregningOppsummering {
  etterbetaling: SimulertEtterbetalingOppsummering
  feilutbetaling: SimulertFeilutbetalingOppsummering | null
}

export interface SimulertEtterbetalingOppsummering {
  brutto: number
  beloepPerKlasseType: SimulertKlasseTypeOppsummering[]
  netto: number
}

export interface SimulertKlasseTypeOppsummering {
  klasseType: SimulertKlasseType
  beloep: number
}

export type SimulertKlasseType = 'YTEL' | 'SKAT' | 'FEIL' | 'MOTP' | 'JUST' | 'TREK'

export interface SimulertFeilutbetalingOppsummering {
  brutto: number
  beloepBrukerenSkulleHatt: number
  netto: number
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
  klasseType: SimulertKlasseType
}
