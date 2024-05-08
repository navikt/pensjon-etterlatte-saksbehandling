import { KildeSaksbehandler } from '~shared/types/kilde'

export interface AktivitetspliktOppfolging {
  behandlingId: string
  aktivitet: string
  opprettet: string
  opprettetAv: string
}

export interface IAktivitet {
  id: string
  sakId: number
  behandlingId: string
  type: AktivitetspliktType
  fom: string
  tom?: string
  opprettet: KildeSaksbehandler
  endret: KildeSaksbehandler
  beskrivelse: string
}

export interface IOpprettAktivitet {
  id: string | undefined
  sakId: number
  type: AktivitetspliktType
  fom: string
  tom?: string
  beskrivelse: string
}

export enum AktivitetspliktType {
  ARBEIDSTAKER = 'ARBEIDSTAKER',
  SELVSTENDIG_NAERINGSDRIVENDE = 'SELVSTENDIG_NAERINGSDRIVENDE',
  ETABLERER_VIRKSOMHET = 'ETABLERER_VIRKSOMHET',
  ARBEIDSSOEKER = 'ARBEIDSSOEKER',
  UTDANNING = 'UTDANNING',
}

export enum AktivitetspliktVurderingType {
  AKTIVITET_UNDER_50 = 'AKTIVITET_UNDER_50',
  AKTIVITET_OVER_50 = 'AKTIVITET_OVER_50',
  AKTIVITET_100 = 'AKTIVITET_100',
}

export const tekstAktivitetspliktVurderingType: Record<AktivitetspliktVurderingType, string> = {
  AKTIVITET_UNDER_50: 'Under 50%',
  AKTIVITET_OVER_50: 'Over 50%',
  AKTIVITET_100: '100%',
}

export interface IAktivitetspliktVurdering {
  id: string
  sakId: number
  behandlingId: string | undefined
  oppgaveId: string
  vurdering: AktivitetspliktVurderingType
  unntak: boolean
  fom: string
  opprettet: KildeSaksbehandler
  endret: KildeSaksbehandler
  beskrivelse: string
}

export interface IOpprettAktivitetspliktVurdering {
  id: string | undefined
  vurdering: AktivitetspliktVurderingType
  unntak: boolean
  fom: string
  beskrivelse: string
}
