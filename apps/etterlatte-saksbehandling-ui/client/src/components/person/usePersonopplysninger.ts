import { useAppSelector } from '~store/Store'
import { Personopplysninger } from '~shared/types/grunnlag'

export function usePersonopplysninger(): Personopplysninger | null {
  return useAppSelector((state) => state.personopplysningerReducer.personopplysninger)
}
