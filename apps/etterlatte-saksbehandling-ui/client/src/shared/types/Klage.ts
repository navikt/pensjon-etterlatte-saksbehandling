import { ISak } from '~shared/types/sak'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { JaNei } from '~shared/types/ISvar'
import { VedtakType } from '~components/vedtak/typer'

export interface Klage {
  id: string
  sak: ISak
  opprettet: string
  status: KlageStatus
  kabalStatus?: KabalStatus
  formkrav?: FormkravMedSaksbehandler
  utfall?: KlageUtfall
}

export enum KlageStatus {
  OPPRETTET = 'OPPRETTET',
  FORMKRAV_OPPFYLT = 'FORMKRAV_OPPFYLT',
  FORMKRAV_IKKE_OPPFYLT = 'FORMKRAV_IKKE_OPPFYLT',
  UTFALL_VURDERT = 'UTFALL_VURDERT',
  FERDIGSTILT = 'FERDIGSTILT',
}

export const teksterKlagestatus: Record<KlageStatus, string> = {
  OPPRETTET: 'Opprettet',
  FORMKRAV_OPPFYLT: 'Formkrav vurdert oppfylt',
  FORMKRAV_IKKE_OPPFYLT: 'Formkrav vurdert ikke oppfylt',
  UTFALL_VURDERT: 'Utfall av klagen vurdert',
  FERDIGSTILT: 'Klagen ferdigstilt i Gjenny',
}

export const enum KabalStatus {}

export interface VedtaketKlagenGjelder {
  id: string
  behandlingId: string
  datoAttestert?: string
  vedtakType?: VedtakType
}

export interface Formkrav {
  vedtaketKlagenGjelder: VedtaketKlagenGjelder | null
  erKlagerPartISaken: JaNei
  erKlagenSignert: JaNei
  gjelderKlagenNoeKonkretIVedtaket: JaNei
  erKlagenFramsattInnenFrist: JaNei
  erFormkraveneOppfylt: JaNei
}

export interface FormkravMedSaksbehandler {
  saksbehandler: KildeSaksbehandler
  formkrav: Formkrav
}

export enum Utfall {
  OMGJOERING = 'OMGJOERING',
  DELVIS_OMGJOERING = 'DELVIS_OMGJOERING',
  STADFESTE_VEDTAK = 'STADFESTE_VEDTAK',
}

export const teksterKlageutfall: Record<Utfall, string> = {
  OMGJOERING: 'Omgjøring',
  DELVIS_OMGJOERING: 'Delvis omgjøring',
  STADFESTE_VEDTAK: 'Stadfeste vedtak',
} as const

export type KlageUtfall =
  | {
      utfall: 'OMGJOERING'
      omgjoering: Omgjoering
      saksbehandler: KildeSaksbehandler
    }
  | {
      utfall: 'DELVIS_OMGJOERING'
      omgjoering: Omgjoering
      innstilling: InnstillingTilKabal
      saksbehandler: KildeSaksbehandler
    }
  | {
      utfall: 'STADFESTE_VEDTAK'
      innstilling: InnstillingTilKabal
      saksbehandler: KildeSaksbehandler
    }

export type KlageUtfallUtenBrev =
  | {
      utfall: 'OMGJOERING'
      omgjoering: Omgjoering
    }
  | {
      utfall: 'DELVIS_OMGJOERING'
      omgjoering: Omgjoering
      innstilling: InnstillingTilKabalUtenBrev
    }
  | {
      utfall: 'STADFESTE_VEDTAK'
      innstilling: InnstillingTilKabalUtenBrev
    }

interface KlageBrevInnstilling {}

interface InnstillingTilKabal {
  lovhjemmel: string
  tekst: string
  brev: KlageBrevInnstilling
}

export type InnstillingTilKabalUtenBrev = Omit<InnstillingTilKabal, 'brev'>

export interface Omgjoering {
  grunnForOmgjoering: AarsakOmgjoering
  begrunnelse: string
}

export const LOVHJEMLER_KLAGE = [
  '§ 1-5',
  '§ 3-5 (trygdetid)',
  '§ 17-2 (EØS-sammenlegging)',
  '§ 17-3',
  '§ 17-4',
  '§ 17-5',
  '§ 17-7',
  '§ 17-8',
  '§ 17-9',
  '§ 17-10',
  '§ 17-11',
  '§ 17-12',
  '§ 17-15',
  '§ 18-2 (EØS-sammenlegging)',
  '§ 18-3',
  '§ 18-4',
  '§ 18-5',
  '§ 18-10',
  '§ 21-12',
  '§ 22-13 3. ledd',
  '§ 22-13 4. ledd c)',
  '§ 22-13 7. ledd',
  '§ 22-15 1. ledd 1. pkt.',
  '§ 22-15 1. ledd 2. pkt.',
  '§ 22-15 2. ledd',
  'Fvl. 33, 2. ledd',
  'Fvl. 35, 1. ledd c)',
  'Kapittel 2',
  'Kapittel 3',
  'Kapittel 17',
  'Kapittel 18',
  'Kapittel 22',
  'Trygdeavtale',
  'EØS',
  'EØS- medlemskap/trygdetid',
] as const

export const AARSAKER_OMGJOERING = [
  'FEIL_LOVANVENDELSE',
  'FEIL_REGELVERKSFORSTAAELSE',
  'FEIL_ELLER_ENDRET_FAKTA',
  'PROSESSUELL_FEIL',
  'SAKEN_HAR_EN_AAPEN_BEHANDLING',
  'ANNET',
] as const

export type AarsakOmgjoering = (typeof AARSAKER_OMGJOERING)[number]

export const TEKSTER_AARSAK_OMGJOERING: Record<AarsakOmgjoering, string> = {
  FEIL_LOVANVENDELSE: 'Feil lovanvendelse',
  FEIL_REGELVERKSFORSTAAELSE: 'Feil regelverksforståelse',
  FEIL_ELLER_ENDRET_FAKTA: 'Feil eller endret fakta',
  PROSESSUELL_FEIL: 'Prosessuell feil',
  SAKEN_HAR_EN_AAPEN_BEHANDLING: 'Søker / part i saken har en åpen behandling',
  ANNET: 'Annet',
} as const
