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
        key={`adoptertav-fornavn`}
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
        key={`adoptertav-mellomnavn`}
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
        key={`adoptertav-etternavn`}
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
`
