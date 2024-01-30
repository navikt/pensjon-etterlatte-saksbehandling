import { ISak } from '~shared/types/sak'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { JaNei } from '~shared/types/ISvar'
import { VedtakType } from '~components/vedtak/typer'

export const enum KabalResultat {
  MEDHOLD = 'MEDHOLD',
  IKKE_MEDHOLD = 'IKKE_MEDHOLD',
  IKKE_MEDHOLD_FORMKRAV_AVVIST = 'IKKE_MEDHOLD_FORMKRAV_AVVIST',
  IKKE_SATT = 'IKKE_SATT',
  HENLAGT = 'HENLAGT',
}

export const teksterKabalUtfall: Record<KabalResultat, string> = {
  HENLAGT: 'Henlagt',
  IKKE_MEDHOLD: 'Ikke medhold',
  IKKE_MEDHOLD_FORMKRAV_AVVIST: 'Ikke medhold (formkrav avvist)',
  IKKE_SATT: 'Ikke satt',
  MEDHOLD: 'Medhold',
}

export interface NyKlageRequestUtfylling {
  mottattDato: string
}

export interface NyKlageRequest extends NyKlageRequestUtfylling {
  sakId: number
  journalpostId: string
  innsender?: string
}

export interface AvbrytKlageRequest {
  klageId: string
  aarsakTilAvbrytelse: AarsakTilAvbrytelse
  kommentar: string
}

export interface Klage {
  id: string
  sak: ISak
  opprettet: string
  status: KlageStatus
  kabalStatus?: KabalStatus
  formkrav?: FormkravMedSaksbehandler
  utfall?: KlageUtfall
  kabalResultat?: KabalResultat
  innkommendeDokument?: InnkommendeKlage
}

export interface InnkommendeKlage {
  mottattDato: string
  journalpostId: string
  innsender?: string
}

export enum KlageStatus {
  OPPRETTET = 'OPPRETTET',
  FORMKRAV_OPPFYLT = 'FORMKRAV_OPPFYLT',
  FORMKRAV_IKKE_OPPFYLT = 'FORMKRAV_IKKE_OPPFYLT',
  UTFALL_VURDERT = 'UTFALL_VURDERT',
  FERDIGSTILT = 'FERDIGSTILT',
  AVBRUTT = 'AVBRUTT',
}

export const teksterKlagestatus: Record<KlageStatus, string> = {
  OPPRETTET: 'Opprettet',
  FORMKRAV_OPPFYLT: 'Formkrav vurdert oppfylt',
  FORMKRAV_IKKE_OPPFYLT: 'Formkrav vurdert ikke oppfylt',
  UTFALL_VURDERT: 'Utfall av klagen vurdert',
  FERDIGSTILT: 'Klagen ferdigstilt i Gjenny',
  AVBRUTT: 'Klagen avbrutt',
}

export const erKlageRedigerbar = (klage: Klage) => {
  const redigerbareStatuser = [
    KlageStatus.OPPRETTET,
    KlageStatus.FORMKRAV_IKKE_OPPFYLT,
    KlageStatus.UTFALL_VURDERT,
    KlageStatus.FORMKRAV_IKKE_OPPFYLT,
  ]
  return redigerbareStatuser.includes(klage.status)
}

export const enum KabalStatus {
  OPPRETTET = 'OPPRETTET',
  UTREDES = 'UTREDES',
  VENTER = 'VENTER',
  FERDIGSTILT = 'FERDIGSTILT',
}

export const teksterKabalstatus: Record<KabalStatus, string> = {
  FERDIGSTILT: 'Ferdigstilt',
  OPPRETTET: 'Opprettet',
  UTREDES: 'Utredes',
  VENTER: 'På vent',
}

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

export enum AarsakTilAvbrytelse {
  BRUKER_HAR_TRUKKET_KLAGEN = 'BRUKER_HAR_TRUKKET_KLAGEN',
  FEILREGISTRERT = 'FEILREGISTRERT',
  ALLEREDE_LOEST = 'ALLEREDE_LOEST',
  ANNET = 'ANNET',
}

export const teksterAarsakTilAvbrytelse: Record<AarsakTilAvbrytelse, string> = {
  BRUKER_HAR_TRUKKET_KLAGEN: 'Klagen er blitt trukket av søker',
  FEILREGISTRERT: 'Opprettet ved en feil',
  ALLEREDE_LOEST: 'Saken er allerede blitt løst',
  ANNET: 'Ingen av alternativene passer',
}

interface KlageBrevInnstilling {
  brevId: number
}

export interface InnstillingTilKabal {
  lovhjemmel: LovhjemmelFelles
  tekst: string
  brev: KlageBrevInnstilling
}

export type InnstillingTilKabalUtenBrev = Omit<InnstillingTilKabal, 'brev'>

export interface Omgjoering {
  grunnForOmgjoering: AarsakOmgjoering
  begrunnelse: string
}

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

export type LovhjemmelFelles = (typeof LOVHJEMLER_FELLES)[number]
export type LovhjemmelKunBp = (typeof LOVHJEMLER_KUN_BP)[number]
export type LovhjemmelKunOms = (typeof LOVHJEMLER_KUN_OMS)[number]
export type LovhjemmelBp = LovhjemmelFelles | LovhjemmelKunBp
export type LovhjemmelOms = LovhjemmelFelles | LovhjemmelKunOms
export type AlleHjemler = LovhjemmelOms | LovhjemmelBp

export const TEKSTER_LOVHJEMLER: Record<AlleHjemler, string> = {
  ANDRE_TRYGDEAVTALER: 'Trygdeavtale',
  EOES_AVTALEN_BEREGNING: 'EØS - beregning',
  EOES_AVTALEN_MEDLEMSKAP_TRYGDETID: 'EØS - medlemskap / trygdetid',
  FTRL_17_10: '§ 17-10',
  FTRL_17_11: '§ 17-11',
  FTRL_17_12: '§ 17-12',
  FTRL_17_13: '§ 17-13',
  FTRL_17_14: '§ 17-14',
  FTRL_17_15: '§ 17-15',
  FTRL_17_1_A: '§ 17-1 a',
  FTRL_17_2: '§ 17-2',
  FTRL_17_3: '§ 17-3',
  FTRL_17_4: '§ 17-4',
  FTRL_17_5: '§ 17-5',
  FTRL_17_6: '§ 17-6',
  FTRL_17_7: '§ 17-7',
  FTRL_17_8: '§ 17-8',
  FTRL_17_9: '§ 17-9',
  FTRL_17_A_1: '§ 17 A-1',
  FTRL_17_A_2: '§ 17 A-2',
  FTRL_17_A_3: '§ 17 A-3',
  FTRL_17_A_4: '§ 17 A-4',
  FTRL_17_A_5: '§ 17 A-5',
  FTRL_17_A_6: '§ 17 A-6',
  FTRL_17_A_7: '§ 17 A-7',
  FTRL_17_A_8: '§ 17 A-8',
  FTRL_18_10: '§ 18-10',
  FTRL_18_11: '§ 18-11',
  FTRL_18_1_A: '§ 18-1 a',
  FTRL_18_2: '§ 18-2',
  FTRL_18_3: '§ 18-3',
  FTRL_18_4: '§ 18-4',
  FTRL_18_5: '§ 18-5',
  FTRL_18_6: '§ 18-6',
  FTRL_18_7: '§ 18-7',
  FTRL_18_8: '§ 18-8',
  FTRL_18_9: '§ 18-9',
  FTRL_1_3: '§ 1-3',
  FTRL_1_3_A: '§ 1-3 a',
  FTRL_1_3_B: '§ 1-3 b',
  FTRL_1_5: '§ 1-5',
  FTRL_21_10: '§ 21-10',
  FTRL_21_6: '§ 21-6',
  FTRL_21_7: '§ 21-7',
  FTRL_22_1: '§ 22-1',
  FTRL_22_12: '§ 22-12',
  FTRL_22_13_1: '§ 22-13 1. ledd',
  FTRL_22_13_3: '§ 22-13 3. ledd',
  FTRL_22_13_4_C: '§ 22-13 4. ledd c)',
  FTRL_22_13_7: '§ 22-13 7. ledd',
  FTRL_22_14_3: '§ 22-14 3. ledd',
  FTRL_22_15_1_1: '§ 22-15 1. ledd 1. pkt.',
  FTRL_22_15_1_2: '§ 22-15 1. ledd 2. pkt',
  FTRL_22_15_2: '§ 22-15 2. ledd',
  FTRL_22_15_4: '§ 22-15 4. ledd',
  FTRL_22_15_5: '§ 22-15 5. ledd',
  FTRL_22_17A: '§ 22-17 a',
  FTRL_22_17B: '§ 22-17 b',
  FTRL_22_1_A: '§ 22-1 a',
  FTRL_25_14: '§ 25-14',
  FTRL_2_1: '§ 2-1',
  FTRL_2_10: '§ 2-10',
  FTRL_2_11: '§ 2-11',
  FTRL_2_12: '§ 2-12',
  FTRL_2_13: '§ 2-13',
  FTRL_2_14: '§ 2-14',
  FTRL_2_15: '§ 2-15',
  FTRL_2_16: '§ 2-16',
  FTRL_2_17: '§ 2-17',
  FTRL_2_1_A: '§ 2-1 a',
  FTRL_2_2: '§ 2-2',
  FTRL_2_3: '§ 2-3',
  FTRL_2_4: '§ 2-4',
  FTRL_2_5: '§ 2-5',
  FTRL_2_6: '§ 2-6',
  FTRL_2_7: '§ 2-7',
  FTRL_2_7_A: '§ 2-7 a',
  FTRL_2_8: '§ 2-8',
  FTRL_2_9: '§ 2-9',
  FTRL_3_1: '§ 3-1',
  FTRL_3_10: '§ 3-10',
  FTRL_3_13: '§ 3-13',
  FTRL_3_14: '§ 3-14',
  FTRL_3_15: '§ 3-15',
  FTRL_3_5_TRYGDETID: '§ 3-5 (trygdetid)',
  FTRL_3_7: '§ 3-7',
  FVL_31: 'Fvl. § 31',
  FVL_32: 'Fvl. § 32',
  FVL_33_2: 'Fvl. § 33, 2. ledd',
  FVL_35_1_C: 'Fvl. § 35, 1. ledd c)',
  FVL_36: 'Fvl. § 36',
}

const LOVHJEMLER_FELLES = [
  'FTRL_1_3',
  'FTRL_1_3_A',
  'FTRL_1_3_B',
  'FTRL_1_5',

  'FTRL_2_1',
  'FTRL_2_1_A',
  'FTRL_2_2',
  'FTRL_2_3',
  'FTRL_2_4',
  'FTRL_2_5',
  'FTRL_2_6',
  'FTRL_2_7',
  'FTRL_2_7_A',
  'FTRL_2_8',
  'FTRL_2_9',
  'FTRL_2_10',
  'FTRL_2_11',
  'FTRL_2_12',
  'FTRL_2_13',
  'FTRL_2_14',
  'FTRL_2_15',
  'FTRL_2_16',
  'FTRL_2_17',

  'FTRL_3_1',
  'FTRL_3_5_TRYGDETID',
  'FTRL_3_7',
  'FTRL_3_10',
  'FTRL_3_13',
  'FTRL_3_14',
  'FTRL_3_15',

  'FTRL_21_6',
  'FTRL_21_7',
  'FTRL_21_10',

  'FTRL_22_1',
  'FTRL_22_1_A',
  'FTRL_22_12',
  'FTRL_22_13_1',
  'FTRL_22_13_3',
  'FTRL_22_13_4_C',
  'FTRL_22_13_7',
  'FTRL_22_14_3',
  'FTRL_22_15_1_1',
  'FTRL_22_15_1_2',
  'FTRL_22_15_2',
  'FTRL_22_15_4',
  'FTRL_22_15_5',
  'FTRL_22_17A',
  'FTRL_22_17B',
  'FTRL_25_14',

  'FVL_31',
  'FVL_32',
  'FVL_33_2',
  'FVL_35_1_C',
  'FVL_36',

  'ANDRE_TRYGDEAVTALER',
  'EOES_AVTALEN_BEREGNING',
  'EOES_AVTALEN_MEDLEMSKAP_TRYGDETID',
] as const

const LOVHJEMLER_KUN_BP = [
  'FTRL_18_1_A',
  'FTRL_18_2',
  'FTRL_18_3',
  'FTRL_18_4',
  'FTRL_18_5',
  'FTRL_18_6',
  'FTRL_18_7',
  'FTRL_18_8',
  'FTRL_18_9',
  'FTRL_18_10',
  'FTRL_18_11',
] as const

const LOVHJEMLER_KUN_OMS = [
  'FTRL_17_1_A',
  'FTRL_17_2',
  'FTRL_17_3',
  'FTRL_17_4',
  'FTRL_17_5',
  'FTRL_17_6',
  'FTRL_17_7',
  'FTRL_17_8',
  'FTRL_17_9',
  'FTRL_17_10',
  'FTRL_17_11',
  'FTRL_17_12',
  'FTRL_17_13',
  'FTRL_17_14',
  'FTRL_17_15',

  'FTRL_17_A_1',
  'FTRL_17_A_2',
  'FTRL_17_A_3',
  'FTRL_17_A_4',
  'FTRL_17_A_5',
  'FTRL_17_A_6',
  'FTRL_17_A_7',
  'FTRL_17_A_8',
] as const

export const LOVHJEMLER_BP = [...LOVHJEMLER_KUN_BP, ...LOVHJEMLER_FELLES] as const
export const LOVHJEMLER_OMS = [...LOVHJEMLER_KUN_OMS, ...LOVHJEMLER_FELLES] as const
