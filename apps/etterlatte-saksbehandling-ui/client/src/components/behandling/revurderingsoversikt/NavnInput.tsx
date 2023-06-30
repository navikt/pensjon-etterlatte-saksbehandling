import { Navn } from '~shared/types/RevurderingInfo'
import React, { useState } from 'react'
import { TextField } from '@navikt/ds-react'
import styled from 'styled-components'

export const NavnInput = (props: { navn: Navn | undefined; update: (n: Navn) => void }) => {
  const { navn, update } = props
  const [fornavn, setFornavn] = useState(navn?.fornavn || '')
  const [mellomnavn, setMellomnavn] = useState(navn?.mellomnavn || '')
  const [etternavn, setEtternavn] = useState(navn?.etternavn || '')

  return (
    <NavnWrapper>
      <TextField
        label={'Fornavn'}
        value={fornavn}
        key={`fornavn-input-label`}
        onChange={(e) => {
          setFornavn(e.target.value)
          if (etternavn) {
            update({
              fornavn: e.target.value,
              mellomnavn: mellomnavn,
              etternavn: etternavn,
            })
          }
        }}
      />
      <TextField
        label={'Mellomnavn'}
        value={mellomnavn}
        key={`mellomnavn-input-label`}
        onChange={(e) => {
          setMellomnavn(e.target.value)
          if (fornavn && etternavn) {
            update({
              fornavn: fornavn,
              mellomnavn: e.target.value,
              etternavn: etternavn,
            })
          }
        }}
      />
      <TextField
        label={'Etternavn'}
        value={etternavn}
        key={`etternavn-input-label`}
        onChange={(e) => {
          setEtternavn(e.target.value)
          if (fornavn && etternavn) {
            update({
              fornavn: fornavn,
              mellomnavn: mellomnavn,
              etternavn: e.target.value,
            })
          }
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
