import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { LandMedDokumenter } from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'

export const SOESKENJUSTERING_GRUNNER = [
  'NYTT_SOESKEN',
  'SOESKEN_DOER',
  'SOESKEN_INN_INSTITUSJON_INGEN_ENDRING',
  'SOESKEN_INN_INSTITUSJON_ENDRING',
  'SOESKEN_UT_INSTITUSJON',
  'FORPLEID_ETTER_BARNEVERNSLOVEN',
  'SOESKEN_BLIR_ADOPTERT',
] as const

export type BarnepensjonSoeskenjusteringGrunn = (typeof SOESKENJUSTERING_GRUNNER)[number]

export interface RevurderingAarsakAnnen {
  type: 'ANNEN'
  aarsak: string
}

export interface SoeskenjusteringInfo {
  type: 'SOESKENJUSTERING'
  grunnForSoeskenjustering: BarnepensjonSoeskenjusteringGrunn
}

export interface AdopsjonInfo {
  type: 'ADOPSJON'
  adoptertAv1: Navn
  adoptertAv2: Navn | undefined
}

export interface OmgjoeringAvFarskapInfo {
  type: 'OMGJOERING_AV_FARSKAP'
  naavaerendeFar: Navn
  forrigeFar: Navn
}

export interface FengselsoppholdInfo {
  type: 'FENGSELSOPPHOLD'
  fraDato: Date
  tilDato: Date
}

export interface InstitusjonsoppholdInfo {
  type: 'INSTITUSJONSOPPHOLD'
  erEtterbetalingMerEnnTreMaaneder: boolean
  prosent: number | undefined
  innlagtdato: Date | undefined
  utskrevetdato: Date | undefined
}

export interface SluttbehandlingUtlandInfo {
  type: 'SLUTTBEHANDLING_UTLAND'
  landMedDokumenter: LandMedDokumenter[]
}

export interface Navn {
  fornavn: string
  mellomnavn: string | undefined
  etternavn: string
}

export type RevurderingInfo =
  | RevurderingAarsakAnnen
  | SoeskenjusteringInfo
  | AdopsjonInfo
  | OmgjoeringAvFarskapInfo
  | FengselsoppholdInfo
  | InstitusjonsoppholdInfo
  | SluttbehandlingUtlandInfo

export interface RevurderingMedBegrunnelse {
  revurderingInfo: RevurderingInfo
  begrunnelse: string | undefined
}

export function hentUndertypeFraBehandling<T extends RevurderingInfo>(
  type: T['type'],
  behandling?: IDetaljertBehandling
): T | null {
  const revurderingInfo = behandling?.revurderinginfo?.revurderingInfo
  if (revurderingInfo?.type === type) {
    return revurderingInfo as T
  }
  return null
}

export const tekstSoeskenjustering: Record<BarnepensjonSoeskenjusteringGrunn, string> = {
  FORPLEID_ETTER_BARNEVERNSLOVEN: 'Forpleid etter barnevernsloven',
  NYTT_SOESKEN: 'Nytt søsken',
  SOESKEN_BLIR_ADOPTERT: 'Søsken har blitt adoptert',
  SOESKEN_DOER: 'Søsken har dødd',
  SOESKEN_INN_INSTITUSJON_ENDRING: 'Søsken har institusjonsopphold som gir reduksjon i ytelsen',
  SOESKEN_INN_INSTITUSJON_INGEN_ENDRING: 'Søsken har institusjonsopphold som ikke gir reduksjon i ytelsen',
  SOESKEN_UT_INSTITUSJON: 'Søsken er ute av et institusjonsopphold',
}
