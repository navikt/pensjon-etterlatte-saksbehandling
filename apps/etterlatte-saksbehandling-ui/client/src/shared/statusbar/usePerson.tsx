import { useAppSelector } from '~store/Store'
import { IPdlPersonNavnFoedsel } from '~shared/types/Person'

export function usePerson(): IPdlPersonNavnFoedsel | null {
  return useAppSelector((state) => state.personReducer.person)
}
