import { useAppSelector } from '~store/Store'
import { Tilbakekreving } from '~shared/types/Tilbakekreving'

export function useTilbakekreving(): Tilbakekreving | null {
  return useAppSelector((state) => state.tilbakekrevingReducer.tilbakekreving)
}
