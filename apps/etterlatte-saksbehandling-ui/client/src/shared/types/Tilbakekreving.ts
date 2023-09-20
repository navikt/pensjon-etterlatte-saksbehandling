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
  fra: string
  til: string
  beskrivelse: string
  bruttoUtbetaling: number
  beregnetNyBrutto: number
  beregnetFeilutbetaling: number
  skatteprosent: number
  buttoTilbakekreving: number
  nettoTilbakekreving: number
  skatt: number
  resultat: string | null
  skyld: string | null
  aarsak: string | null
}

export enum TilbakekrevingStatus {
  OPPRETTET = 'OPPRETTET',
}
