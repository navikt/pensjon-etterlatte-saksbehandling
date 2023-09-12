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

export type KlageUtfall = {}
