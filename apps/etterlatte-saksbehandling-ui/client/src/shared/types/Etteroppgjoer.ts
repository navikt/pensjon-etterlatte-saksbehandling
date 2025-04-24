import { ISak } from '~shared/types/sak'
import { IAvkortetYtelse, IAvkortingGrunnlag } from '~shared/types/IAvkorting'
import { GrunnlagKilde } from '~shared/types/grunnlag'

export interface Etteroppgjoer {
  behandling: EtteroppgjoerBehandling
  opplysninger: EtteroppgjoerOpplysninger
  faktiskInntekt?: FaktiskInntekt
  avkortingFaktiskInntekt: Avkorting | undefined
  beregnetEtteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto | undefined
}

export interface EtteroppgjoerBehandling {
  id: string
  status: EtteroppgjoerBehandlingStatus
  relatertForbehandlingId: string // burde bruke denne for å hente faktiskInntekt etteroppgjør revurdering
  sak: ISak
  aar: number
  innvilgetPeriode: {
    fom: string
    tom: string
  }
  opprettet: string // Mottatt?
  brevId?: number
}

export enum EtteroppgjoerBehandlingStatus {
  OPPRETTET = 'OPPRETTET',
  BEREGNET = 'BEREGNET',
  FERDIGSTILT = 'FERDIGSTILT',
}

export const teksterEtteroppgjoerBehandlingStatus: Record<EtteroppgjoerBehandlingStatus, string> = {
  OPPRETTET: 'Opprettet',
  BEREGNET: 'Beregnet',
  FERDIGSTILT: 'Ferdigstilt',
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
  spesifikasjon: string
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

export enum EtteroppgjoerResultatType {
  TILBAKEKREVING = 'TILBAKEKREVING',
  ETTERBETALING = 'ETTERBETALING',
  IKKE_ETTEROPPGJOER = 'IKKE_ETTEROPPGJOER',
}

export interface BeregnetEtteroppgjoerResultatDto {
  id: string
  aar: number
  forbehandlingId: string
  sisteIverksatteBehandlingId: string
  utbetaltStoenad: number
  nyBruttoStoenad: number
  differanse: number
  grense: EtteroppgjoerGrenseDto
  resultatType: EtteroppgjoerResultatType
  tidspunkt: string
  kilde: GrunnlagKilde
  avkortingForbehandlingId: string
  avkortingSisteIverksatteId: string
}

interface EtteroppgjoerGrenseDto {
  tilbakekreving: number
  etterbetaling: number
  rettsgebyr: number
  rettsgebyrGyldigFra: string
}
