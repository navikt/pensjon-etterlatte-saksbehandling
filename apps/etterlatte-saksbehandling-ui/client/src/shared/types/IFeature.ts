export interface IFeature {
  toggle: string
  enabled: Status
}

export enum Status {
  PAA = 'PAA',
  AV = 'AV',
  UDEFINERT = 'UDEFINERT',
  HENTING_FEILA = 'HENTING_FEILA',
}
