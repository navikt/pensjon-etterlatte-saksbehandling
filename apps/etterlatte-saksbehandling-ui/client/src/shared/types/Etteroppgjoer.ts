import { ISak } from '~shared/types/sak'
import { IAvkortetYtelse, IAvkortingGrunnlag } from '~shared/types/IAvkorting'

export interface Etteroppgjoer {
  behandling: EtteroppgjoerBehandling
  opplysninger: EtteroppgjoerOpplysninger
  avkortingFaktiskInntekt: Avkorting | undefined
}

export interface EtteroppgjoerBehandling {
  id: string
  status: string
  sak: ISak
  aar: number
  innvilgetPeriode: {
    fom: string
    tom: string
  }
  opprettet: string // Mottatt?
}

export interface EtteroppgjoerOpplysninger {
  skatt: PensjonsgivendeInntektFraSkatteetaten
  ainntekt: AInntekt
  tidligereAvkorting: Avkorting
}

export interface FaktiskInntekt {
  loennsinntekt: number
  afp: number
  naeringsinntekt: number
  utland: number
}

export interface PensjonsgivendeInntektFraSkatteetaten {
  inntekter: PensjonsgivendeInntekt[]
}

export interface PensjonsgivendeInntekt {
  skatteordning: string
  loensinntekt: number
  naeringsinntekt: number
  annet: number
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

export enum UtfallAvForbehandling {
  SEND_VARSELBREV,
  SEND_INFORMASJONSBREV,
  FERDIGSTILL_UTEN_ENDRING,
}
