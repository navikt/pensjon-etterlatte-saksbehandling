import { ISak } from '~shared/types/sak'

export interface Klage {
  id: string
  sak: ISak
  opprettet: string
  status: KlageStatus
  kabalStatus?: KabalStatus
  formkrav?: Formkrav
  utfall?: KlageUtfall
}

export enum KlageStatus {
  OPPRETTET = 'OPPRETTET',
  FORMKRAV_OPPFYLT = 'FORMKRAV_OPPFYLT',
  FORMKRAV_IKKE_OPPFYLT = 'FORMKRAV_IKKE_OPPFYLT',
  UTFALL_VURDERT = 'UTFALL_VURDERT',
  FERDIGSTILT = 'FERDIGSTILT',
}

export const enum KabalStatus {}

export type Formkrav = {}
export type KlageUtfall = {}
