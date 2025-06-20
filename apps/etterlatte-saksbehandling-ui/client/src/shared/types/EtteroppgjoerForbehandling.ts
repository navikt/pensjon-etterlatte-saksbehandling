import { ISak } from '~shared/types/sak'
import { IAvkortetYtelse, IAvkortingGrunnlag } from '~shared/types/IAvkorting'
import { GrunnlagKilde } from '~shared/types/grunnlag'
import { JaNei } from '~shared/types/ISvar'

export interface Etteroppgjoer {
  inntektsaar: number
  status: string
}

export interface EtteroppgjoerForbehandling {
  behandling: EtteroppgjoerBehandling
  opplysninger: EtteroppgjoerOpplysninger
  faktiskInntekt?: FaktiskInntekt
  beregnetEtteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto | undefined
}

export interface EtteroppgjoerBehandling {
  id: string
  status: EtteroppgjoerBehandlingStatus
  sak: ISak
  aar: number
  innvilgetPeriode: {
    fom: string
    tom: string
  }
  opprettet: string // Mottatt?
  brevId?: number
  kopiertFra?: string
  harMottattNyInformasjon?: JaNei
  endringErTilUgunstForBruker?: JaNei
  beskrivelseAvUgunst?: string
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

export interface IInformasjonFraBruker {
  harMottattNyInformasjon: JaNei
  endringErTilUgunstForBruker?: JaNei
  beskrivelseAvUgunst?: string
}

export interface FaktiskInntekt {
  loennsinntekt: number
  afp: number
  naeringsinntekt: number
  utlandsinntekt: number
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
  INGEN_ENDRING = 'INGEN_ENDRING',
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
