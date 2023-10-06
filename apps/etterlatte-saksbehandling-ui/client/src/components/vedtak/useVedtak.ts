import { useAppSelector } from '~store/Store'
import { VedtakSammendrag } from '~components/vedtak/typer'

export function useVedtak(): VedtakSammendrag | null {
  return useAppSelector((state) => state.vedtakReducer.vedtak)
}
