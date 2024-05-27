import { VedtakSammendrag, VedtakType } from '~components/vedtak/typer'

export const hentSisteVedtak = (vedtak: VedtakSammendrag[]): VedtakSammendrag | undefined => {
  return vedtak
    .filter((vedtak) => ![VedtakType.AVVIST_KLAGE, VedtakType.TILBAKEKREVING].includes(vedtak.vedtakType!))
    .filter((vedtak) => !!vedtak.datoAttestert)
    .sort((a, b) => new Date(a.datoAttestert!).valueOf() - new Date(b.datoAttestert!).valueOf())
    .pop()
}

export const hentFoersteVedtak = (vedtak: VedtakSammendrag[]): VedtakSammendrag | undefined => {
  return vedtak
    .filter((vedtak) => vedtak.vedtakType == VedtakType.INNVILGELSE)
    .filter((vedtak) => !!vedtak.datoAttestert)
    .pop()
}
