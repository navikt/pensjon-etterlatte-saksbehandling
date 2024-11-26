import { FeatureToggle } from '~useUnleash'

export interface IFeature {
  toggle: FeatureToggle
  enabled: Status
}

export enum Status {
  PAA = 'PAA',
  AV = 'AV',
  UDEFINERT = 'UDEFINERT',
  HENTING_FEILA = 'HENTING_FEILA',
}
