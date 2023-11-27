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
