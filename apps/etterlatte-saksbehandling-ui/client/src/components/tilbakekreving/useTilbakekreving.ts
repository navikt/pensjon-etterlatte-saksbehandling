import { useAppSelector } from '~store/Store'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'

export function useTilbakekreving(): TilbakekrevingBehandling | null {
  return useAppSelector((state) => state.tilbakekrevingReducer.tilbakekrevingBehandling)
}
