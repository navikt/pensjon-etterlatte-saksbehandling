import { useAppSelector } from '~store/Store'
import { ISjekkliste } from '~shared/types/Sjekkliste'

export function useSjekkliste(): ISjekkliste | null {
  return useAppSelector((state) => state.sjekklisteReducer.sjekkliste)
}
