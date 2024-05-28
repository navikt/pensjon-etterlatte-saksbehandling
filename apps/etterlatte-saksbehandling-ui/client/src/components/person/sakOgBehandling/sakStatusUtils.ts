import { VedtakSammendrag, VedtakType } from '~components/vedtak/typer'

export const hentLoependeVedtak = (vedtak: VedtakSammendrag[]): VedtakSammendrag | undefined => {
  return vedtak
    .filter((vedtak) => ![VedtakType.AVVIST_KLAGE, VedtakType.TILBAKEKREVING].includes(vedtak.vedtakType!))
    .filter((vedtak) => !!vedtak.datoAttestert)
    .sort((a, b) => new Date(a.datoAttestert!).valueOf() - new Date(b.datoAttestert!).valueOf())
    .pop()
}

export const hentInnvilgelseVedtak = (vedtak: VedtakSammendrag[]): VedtakSammendrag | undefined => {
  return vedtak
    .filter((vedtak) => vedtak.vedtakType == VedtakType.INNVILGELSE)
    .filter((vedtak) => !!vedtak.datoAttestert)
    .pop()
}
