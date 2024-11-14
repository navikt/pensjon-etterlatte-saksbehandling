export enum Distribusjonskanal {
  LOKAL_PRINT = 'LOKAL_PRINT',
  PRINT = 'PRINT',
  DITT_NAV = 'DITT_NAV',
  SDP = 'SDP',
  INGEN_DISTRIBUSJON = 'INGEN_DISTRIBUSJON',
  TRYGDERETTEN = 'TRYGDERETTEN',
  DPVT = 'DPVT',
}

export interface DokDistKanalResponse {
  distribusjonskanal: Distribusjonskanal
  regel: string
  regelBegrunnelse: string
}
