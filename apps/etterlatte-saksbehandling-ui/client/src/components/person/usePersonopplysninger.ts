import { useAppSelector } from '~store/Store'
import { Personopplysning, Personopplysninger } from '~shared/types/grunnlag'

export function usePersonopplysninger(): Personopplysninger | null {
  return useAppSelector((state) => state.personopplysningerReducer.personopplysninger)
}

export function usePersonopplysningerOmsAvdoede(): Personopplysning | null {
  return useAppSelector((state) => state.personopplysningerReducer.personopplysninger?.avdoede.find((po) => po) ?? null)
}
