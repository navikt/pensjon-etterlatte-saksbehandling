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

export interface SoeskenjusteringInfo {
  type: 'SOESKENJUSTERING'
  grunnForSoeskenjustering: BarnepensjonSoeskenjusteringGrunn
}

export interface AdopsjonInfo {
  type: 'ADOPSJON'
  adoptertAv: Navn
}

export interface Navn {
  fornavn: String
  mellomnavn: String | undefined
  etternavn: String
}

export type RevurderingInfo = SoeskenjusteringInfo | AdopsjonInfo

export const tekstSoeskenjustering: Record<BarnepensjonSoeskenjusteringGrunn, string> = {
  FORPLEID_ETTER_BARNEVERNSLOVEN: 'Forpleid etter barnevernsloven',
  NYTT_SOESKEN: 'Nytt søsken',
  SOESKEN_BLIR_ADOPTERT: 'Søsken har blitt adoptert',
  SOESKEN_DOER: 'Søsken har dødd',
  SOESKEN_INN_INSTITUSJON_ENDRING: 'Søsken har institusjonsopphold som gir reduksjon i ytelsen',
  SOESKEN_INN_INSTITUSJON_INGEN_ENDRING: 'Søsken har institusjonsopphold som ikke gir reduksjon i ytelsen',
  SOESKEN_UT_INSTITUSJON: 'Søsken er ute av et institusjonsopphold',
}
