import { IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { createAction, createReducer } from '@reduxjs/toolkit'
import { useAppSelector } from '~store/Store'

const initialState: IAktivitetspliktVurderingNyDto = {} as IAktivitetspliktVurderingNyDto

export const setVurderingBehandling = createAction<IAktivitetspliktVurderingNyDto>('set/IAktivitetspliktVurderingNyDto')

export const aktivitetspliktBehandlingReducer = createReducer(initialState, (builder) => {
  builder.addCase(setVurderingBehandling, (_, action) => action.payload)
})

export function useAktivitetspliktBehandlingState(): IAktivitetspliktVurderingNyDto {
  return useAppSelector((state) => state.aktivitetspliktBehandlingReducer)
}
