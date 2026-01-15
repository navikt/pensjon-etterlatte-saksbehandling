export interface VedtakSammendrag {
  id: string
  behandlingId: string
  vedtakType?: VedtakType
  behandlendeSaksbehandler?: string
  attesterendeSaksbehandler?: string
  datoAttestert?: string
  datoFattet?: string
  virkningstidspunkt?: string
  opphoerFraOgMed?: string
}

export enum VedtakType {
  INNVILGELSE = 'INNVILGELSE',
  OPPHOER = 'OPPHOER',
  AVSLAG = 'AVSLAG',
  ENDRING = 'ENDRING',
  TILBAKEKREVING = 'TILBAKEKREVING',
  AVVIST_KLAGE = 'AVVIST_KLAGE',
  INGEN_ENDRING = 'INGEN_ENDRING',
}

export interface Samordningsvedtak {
  samordningVedtakId: number
  saksId: number
  saksKode: string
  vedtakId: number
  vedtakstatusKode: string
  etterbetaling: boolean
  utvidetSamordningsfrist: boolean
  virkningFom: string
  virkningTom?: string
  samordningsmeldinger: Samordningsmelding[]
  behandlingId: string
}

export interface Samordningsmelding {
  samId: number
  meldingstatusKode: string
  tpNr: string
  tpNavn: string
  sendtDato: string
  svartDato?: string
  purretDato?: string
  refusjonskrav: boolean
}

export interface OppdaterSamordningsmelding {
  samId: number
  pid: string
  tpNr: string
  refusjonskrav: boolean
  kommentar: string
  vedtakId: number
}
