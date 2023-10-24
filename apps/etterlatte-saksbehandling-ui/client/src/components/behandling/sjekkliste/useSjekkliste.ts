import { useAppSelector } from '~store/Store'
import { ISjekkliste } from '~shared/types/Sjekkliste'

export function useSjekkliste(): ISjekkliste | null {
  return useAppSelector((state) => state.sjekklisteReducer.sjekkliste)
}
export function useSjekklisteValideringsfeil(): String[] {
  return useAppSelector((state) => state.sjekklisteReducer.valideringsfeil)
}
