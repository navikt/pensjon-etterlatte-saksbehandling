import { Navn } from '~shared/types/RevurderingInfo'
import React from 'react'
import { TextField } from '@navikt/ds-react'
import styled from 'styled-components'

export function standardnavn(): Navn {
  return {
    fornavn: '',
    mellomnavn: undefined,
    etternavn: '',
  }
}
export const NavnInput = (props: { navn: Navn; update: (n: Navn) => void }) => {
  const { navn, update } = props

  return (
    <NavnWrapper>
      <TextField
        label={'Fornavn'}
        value={navn?.fornavn}
        key={`fornavn-input-label`}
        onChange={(e) => {
          update({
            ...navn,
            fornavn: e.target.value,
          })
        }}
      />
      <TextField
        label={'Mellomnavn'}
        value={navn?.mellomnavn}
        key={`mellomnavn-input-label`}
        onChange={(e) => {
          update({
            ...navn,
            mellomnavn: e.target.value,
          })
        }}
      />
      <TextField
        label={'Etternavn'}
        value={navn?.etternavn}
        key={`etternavn-input-label`}
        onChange={(e) => {
          update({
            ...navn,
            etternavn: e.target.value,
          })
        }}
      />
    </NavnWrapper>
  )
}

const NavnWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 1rem;
  padding-right: 1rem;
  margin-top: 1rem;
  padding-bottom: 2rem;
`
