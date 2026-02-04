import { ISak } from '~shared/types/sak'
import { IAvkortetYtelse, IAvkortingGrunnlag } from '~shared/types/IAvkorting'
import { GrunnlagKilde } from '~shared/types/grunnlag'
import { JaNei } from '~shared/types/ISvar'

export interface Etteroppgjoer {
  inntektsaar: number
  status: EtteroppgjoerStatus
  sakId: number
}

enum EtteroppgjoerStatus {
  VENTER_PAA_SKATTEOPPGJOER = 'VENTER_PAA_SKATTEOPPGJOER',
  MOTTATT_SKATTEOPPGJOER = 'MOTTATT_SKATTEOPPGJOER',
  VENTER_PAA_SVAR = 'VENTER_PAA_SVAR',
  UNDER_FORBEHANDLING = 'UNDER_FORBEHANDLING',
  UNDER_REVURDERING = 'UNDER_REVURDERING',
  FERDIGSTILT = 'FERDIGSTILT',
  OMGJOERING = 'OMGJOERING',
}

export interface EtteroppgjoerForbehandling {
  id: string
  status: EtteroppgjoerForbehandlingStatus
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
  harVedtakAvTypeOpphoer?: boolean
  opphoerSkyldesDoedsfall?: JaNei
  opphoerSkyldesDoedsfallIEtteroppgjoersaar?: JaNei
  mottattSkatteoppgjoer: boolean
}

export interface DetaljertEtteroppgjoerForbehandling {
  forbehandling: EtteroppgjoerForbehandling
  opplysninger: EtteroppgjoerOpplysninger
  faktiskInntekt?: FaktiskInntekt
  beregnetEtteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto | undefined
}

export enum EtteroppgjoerForbehandlingStatus {
  OPPRETTET = 'OPPRETTET',
  BEREGNET = 'BEREGNET',
  FERDIGSTILT = 'FERDIGSTILT',
  AVBRUTT = 'AVBRUTT',
}

export function kanRedigereEtteroppgjoerForbehandling(status: EtteroppgjoerForbehandlingStatus): boolean {
  return [EtteroppgjoerForbehandlingStatus.OPPRETTET, EtteroppgjoerForbehandlingStatus.BEREGNET].includes(status)
}

export const teksterEtteroppgjoerBehandlingStatus: Record<EtteroppgjoerForbehandlingStatus, string> = {
  OPPRETTET: 'Opprettet',
  BEREGNET: 'Beregnet',
  FERDIGSTILT: 'Ferdigstilt',
  AVBRUTT: 'Avbrutt',
}

export interface Inntektsmaaned {
  maaned: string
  beloep: number
}

export interface InntektSummert {
  filter: string
  inntekter: Inntektsmaaned[]
}

export interface SummerteInntekterAOrdningen {
  afp: InntektSummert
  loenn: InntektSummert
  oms: InntektSummert
  tidspunktBeregnet: string
}

export interface EtteroppgjoerOpplysninger {
  summertPgi?: PensjonsgivendeInntektFraSkatteetatenSummert
  summerteInntekter?: SummerteInntekterAOrdningen
  tidligereAvkorting?: Avkorting
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

export interface PensjonsgivendeInntektFraSkatteetatenSummert {
  loensinntekt: number
  naeringsinntekt: number
  fiskeFangstFamiliebarnehage: number
}

export interface Avkorting {
  avkortingGrunnlag: IAvkortingGrunnlag[]
  avkortetYtelse: IAvkortetYtelse[]
}

export enum EtteroppgjoerResultatType {
  TILBAKEKREVING = 'TILBAKEKREVING',
  ETTERBETALING = 'ETTERBETALING',
  INGEN_ENDRING_MED_UTBETALING = 'INGEN_ENDRING_MED_UTBETALING',
  INGEN_ENDRING_UTEN_UTBETALING = 'INGEN_ENDRING_UTEN_UTBETALING',
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

export interface AvbrytEtteroppgjoerForbehandlingRequest {
  aarsakTilAvbrytelse: AarsakTilAvsluttingEtteroppgjoerForbehandling
  kommentar: string
}

export enum AarsakTilAvsluttingEtteroppgjoerForbehandling {
  IKKE_LENGER_AKTUELL = 'IKKE_LENGER_AKTUELL',
  FEILREGISTRERT = 'FEILREGISTRERT',
  AVBRUTT_PAA_GRUNN_AV_FEIL = 'AVBRUTT_PAA_GRUNN_AV_FEIL',
  ANNET = 'ANNET',
}

export const tekstAarsakTilAvsluttingEtteroppgjoerForbehandling: Record<
  AarsakTilAvsluttingEtteroppgjoerForbehandling,
  string
> = {
  IKKE_LENGER_AKTUELL: 'Ikke lengre aktuell',
  FEILREGISTRERT: 'Feilregistrert',
  AVBRUTT_PAA_GRUNN_AV_FEIL: 'Avbrutt på grunn av feil',
  ANNET: 'Annet',
}
