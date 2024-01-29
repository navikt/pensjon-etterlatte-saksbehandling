export interface VedtakSammendrag {
  id: string
  behandlingId: string
  vedtakType?: VedtakType
  behandlendeSaksbehandler?: string
  attesterendeSaksbehandler?: string
  datoAttestert?: string
  datoFattet?: string
}

export enum VedtakType {
  INNVILGELSE = 'INNVILGELSE',
  OPPHOER = 'OPPHOER',
  AVSLAG = 'AVSLAG',
  ENDRING = 'ENDRING',
  TILBAKEKREVING = 'TILBAKEKREVING ',
}

export interface Samordningsvedtak {
  samordningVedtakId: string
  saksId: string
  saksKode: string
  vedtakId: string
  vedtakstatusKode: string
  etterbetaling: boolean
  utvidetSamordningsfrist: boolean
  virkningFom: string
  virkningTom?: string
  samordningsmeldinger: Samordningsmelding[]
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
