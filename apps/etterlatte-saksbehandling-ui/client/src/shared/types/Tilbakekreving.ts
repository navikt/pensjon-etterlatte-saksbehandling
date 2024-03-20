import { ISak } from '~shared/types/sak'
import { JaNei } from '~shared/types/ISvar'

export interface TilbakekrevingBehandling {
  id: string
  status: TilbakekrevingStatus
  sak: ISak
  opprettet: string
  tilbakekreving: Tilbakekreving
}

export interface Tilbakekreving {
  vurdering?: TilbakekrevingVurdering | null
  perioder: TilbakekrevingPeriode[]
}

export interface TilbakekrevingVurdering {
  aarsak: TilbakekrevingAarsak | null
  beskrivelse: string | null
  forhaandsvarsel: TilbakekrevingVarsel | null
  forhaandsvarselDato: string | null
  doedsbosak: JaNei | null
  foraarsaketAv: string | null
  tilsvar: TilbakekrevingTilsvar | null
  rettsligGrunnlag: TilbakekrevingRettsligGrunnlag | null
  objektivtVilkaarOppfylt: string | null
  subjektivtVilkaarOppfylt: string | null
  uaktsomtForaarsaketFeilutbetaling: string | null
  burdeBrukerForstaatt: string | null
  burdeBrukerForstaattEllerUaktsomtForaarsaket: string | null
  vilkaarsresultat: TilbakekrevingVilkaar | null
  beloepBehold: TilbakekrevingBeloepBehold | null
  reduseringAvKravet: string | null
  foreldet: string | null
  rentevurdering: string | null
  vedtak: string | null
  vurderesForPaatale: string | null
}

export interface TilbakekrevingTilsvar {
  tilsvar: JaNei | null
  dato: string | null
  beskrivelse: string | null
}

export interface TilbakekrevingPeriode {
  maaned: Date
  ytelse: TilbakekrevingBeloep
}

export interface TilbakekrevingBeloep {
  bruttoUtbetaling: number
  nyBruttoUtbetaling: number
  skatteprosent: number
  beregnetFeilutbetaling: number | null
  bruttoTilbakekreving: number | null
  nettoTilbakekreving: number | null
  skatt: number | null
  skyld: TilbakekrevingSkyld | null
  resultat: TilbakekrevingResultat | null
  tilbakekrevingsprosent: number | null
  rentetillegg: number | null
}

export enum TilbakekrevingVarsel {
  EGET_BREV = 'EGET_BREV',
  MED_I_ENDRINGSBREV = 'MED_I_ENDRINGSBREV',
}

export const teksterTilbakekrevingVarsel: Record<TilbakekrevingVarsel, string> = {
  EGET_BREV: 'Sendt i eget brev',
  MED_I_ENDRINGSBREV: 'Sendt som vedlegg i endringsbrev',
} as const

export enum TilbakekrevingAarsak {
  OMGJOERING = 'OMGJOERING',
  OPPHOER = 'OPPHOER',
  REVURDERING = 'REVURDERING',
  UTBFEILMOT = 'UTBFEILMOT',
  ANNET = 'ANNET',
}

export enum TilbakekrevingVilkaar {
  OPPFYLT = 'OPPFYLT',
  DELVIS_OPPFYLT = 'DELVIS_OPPFYLT',
  IKKE_OPPFYLT = 'IKKE_OPPFYLT',
}

export const teksterTilbakekrevingVilkaar: Record<TilbakekrevingVilkaar, string> = {
  OPPFYLT: 'Vilkår oppfylt for hele perioden',
  DELVIS_OPPFYLT: 'Vilkår oppfylt for deler av perioden',
  IKKE_OPPFYLT: 'Vilkår ikke oppfylt',
} as const

export interface TilbakekrevingBeloepBehold {
  behold: TilbakekrevingBeloepBeholdSvar | null
  beskrivelse: string | null
}

export enum TilbakekrevingBeloepBeholdSvar {
  BELOEP_I_BEHOLD = 'BELOEP_I_BEHOLD',
  BELOEP_IKKE_I_BEHOLD = 'BELOEP_IKKE_I_BEHOLD',
}

export const teksterTilbakekrevingBeloepBehold: Record<TilbakekrevingBeloepBeholdSvar, string> = {
  BELOEP_I_BEHOLD: 'Beløp helt eller delvis i behold',
  BELOEP_IKKE_I_BEHOLD: 'Beløp ikke i behold',
} as const

export const teksterTilbakekrevingAarsak: Record<TilbakekrevingAarsak, string> = {
  OMGJOERING: 'Omgjøring',
  OPPHOER: 'Opphør',
  REVURDERING: 'Revurdering',
  UTBFEILMOT: 'Utbetaling til feil mottaker',
  ANNET: 'Annet',
} as const

export enum TilbakekrevingStatus {
  OPPRETTET = 'OPPRETTET',
  UNDER_ARBEID = 'UNDER_ARBEID',
  FATTET_VEDTAK = 'FATTET_VEDTAK',
  ATTESTERT = 'ATTESTERT',
  UNDERKJENT = 'UNDERKJENT',
}
export const teksterTilbakekrevingStatus: Record<TilbakekrevingStatus, string> = {
  OPPRETTET: 'Opprettet',
  UNDER_ARBEID: 'Under arbeid',
  FATTET_VEDTAK: 'Fattet vedtak',
  ATTESTERT: 'Attestert',
  UNDERKJENT: 'Underkjent',
}

export const erUnderBehandling = (status: TilbakekrevingStatus) =>
  status === TilbakekrevingStatus.OPPRETTET ||
  status === TilbakekrevingStatus.UNDER_ARBEID ||
  status === TilbakekrevingStatus.UNDERKJENT

export enum TilbakekrevingSkyld {
  BRUKER = 'BRUKER',
  IKKE_FORDELT = 'IKKE_FORDELT',
  NAV = 'NAV',
  SKYLDDELING = 'SKYLDDELING',
}

export const teksterTilbakekrevingSkyld: Record<TilbakekrevingSkyld, string> = {
  BRUKER: 'Bruker',
  IKKE_FORDELT: 'Ikke fordelt',
  NAV: 'Nav',
  SKYLDDELING: 'Skylddeling',
}

export enum TilbakekrevingResultat {
  DELVIS_TILBAKEKREV = 'DELVIS_TILBAKEKREV',
  FEILREGISTRERT = 'FEILREGISTRERT',
  FORELDET = 'FORELDET',
  FULL_TILBAKEKREV = 'FULL_TILBAKEKREV',
  INGEN_TILBAKEKREV = 'INGEN_TILBAKEKREV',
}

export const teksterTilbakekrevingResultat: Record<TilbakekrevingResultat, string> = {
  INGEN_TILBAKEKREV: 'Ingen tilbakekreving',
  DELVIS_TILBAKEKREV: 'Delvis tilbakekreving',
  FULL_TILBAKEKREV: 'Full tilbakekreving',
  FEILREGISTRERT: 'Feilregistrert',
  FORELDET: 'Foreldet',
}

export enum TilbakekrevingRettsligGrunnlag {
  TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM = 'TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM',
  TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM = 'TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM',
  TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM = 'TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM',
}

export const teksterTilbakekrevingHjemmel: Record<TilbakekrevingRettsligGrunnlag, string> = {
  TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM: 'Folketrygdloven § 22-15 første ledd, første punktum',
  TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM: 'Folketrygdloven § 22-15 første ledd, andre punktum',
  TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM:
    'Kombinasjon folketrygdloven § 22-15 første ledd, første og andre punktum ',
} as const
