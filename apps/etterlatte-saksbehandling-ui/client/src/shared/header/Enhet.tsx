import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { Select } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { IEnhet } from '~store/reducers/SaksbehandlerReducer'

const isNotValid = (value: string | null): value is string => [null, undefined, ''].includes(value)

export const Enhet = () => {
  const [valgtEnhet, setValgtEnhet] = useState<IEnhet | undefined>()
  const { enheter } = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)

  useEffect(() => {
    const enhet = localStorage.getItem('enhet')
    if (isNotValid(enhet)) {
        if (enheter.length) setValgtEnhet(enheter[0])
    } else {
        setValgtEnhet(JSON.parse(String(enhet)))
    }
  }, [])

  useEffect(() => {
    if (valgtEnhet === undefined && localStorage.getItem('enhet') !== undefined) return
    localStorage.setItem('enhet', JSON.stringify(valgtEnhet))
  }, [valgtEnhet])

  return (
    <EnhetWrap>
      <Select
        label={''}
        value={valgtEnhet ? valgtEnhet.enhetId : ''}
        key={'Enhet'}
        onChange={(e) => {
          setValgtEnhet(hentEnhet(enheter, e.target.value))
        }}
        hideLabel={true}
      >
        {enheter.map((enhet: IEnhet) => {
          return (
            <option key={enhet.enhetId} value={enhet.enhetId}>
              {`${enhet.enhetId} ${enhet.navn}`}
            </option>
          )
        })}
      </Select>
    </EnhetWrap>
  )
}

const EnhetWrap = styled.div`
  padding: 0.3em;
`

const hentEnhet = (enheter: IEnhet[], enhetsId: string): IEnhet | undefined =>
  enheter.find((enhet) => enhet.enhetId === enhetsId)
