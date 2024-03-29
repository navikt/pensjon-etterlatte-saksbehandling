import { Klage } from '~shared/types/Klage'
import { useAppSelector } from '~store/Store'

export function useKlage(): Klage | null {
  return useAppSelector((state) => state.klageReducer.klage)
}

export function useKlageRedigerbar(): boolean | null {
  return useAppSelector((state) => state.klageReducer.redigerbar)
}
