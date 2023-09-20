import { ISak } from '~shared/types/sak'

export interface Tilbakekreving {
  id: string
  status: TilbakekrevingStatus
  sak: ISak
  opprettet: string
  kravgrunnlag: Kravgrunnlag
}

export interface Kravgrunnlag {
  perioder: KravgrunnlagPeriode[]
}

export interface KravgrunnlagPeriode {
  periode: {
    fra: string
    til: string
  }
  skatt: number
  grunnlagsbeloep: Grunnlagsbeloep[]
}

export interface Grunnlagsbeloep {
  type: string
  bruttoUtbetaling: number
  beregnetNyBrutto: number
  buttoTilbakekreving: number
  nettoTilbakekreving: number
  beregnetFeilutbetaling: number
  skatteprosent: number
  resultat: string | null
  skyld: string | null
  aarsak: string | null
}

export enum TilbakekrevingStatus {
  OPPRETTET = 'OPPRETTET',
}
