import { ISak } from '~shared/types/sak'
import { IAvkortetYtelse, IAvkortingGrunnlag } from '~shared/types/IAvkorting'

export interface Etteroppgjoer {
  behandling: EtteroppgjoerBehandling
  opplysninger: EtteroppgjoerOpplysninger
}

export interface EtteroppgjoerBehandling {
  id: string
  status: string
  sak: ISak
  aar: number
  opprettet: string // Mottatt?
}

export interface EtteroppgjoerOpplysninger {
  skatt: OpplysnignerSkatt
  ainntekt: AInntekt
  tidligereAvkorting: Avkorting
}

export interface OpplysnignerSkatt {
  aarsinntekt: number
}

export interface AInntekt {
  inntektsmaaneder: AInntektMaaned[]
}

export interface AInntektMaaned {
  maaned: string
  summertBeloep: number
}

export interface Avkorting {
  avkortingGrunnlag: IAvkortingGrunnlag[]
  avkortetYtelse: IAvkortetYtelse[]
}
