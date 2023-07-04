export interface VedtakSammendrag {
  id: string
  behandlingId: string
  type?: VedtakType
  saksbehandlerId?: string
  attestant?: string
  datoAttestert?: string
  datoFattet?: string
}

export enum VedtakType {
  INNVILGELSE = 'INNVILGELSE',
  OPPHOER = 'OPPHOER',
  AVSLAG = 'AVSLAG',
  ENDRING = 'ENDRING',
}
