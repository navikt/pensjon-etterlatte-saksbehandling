import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { Select } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { IEnhet } from '~store/reducers/SaksbehandlerReducer'
import { LocalStorageKeys } from "~shared/types/LocalStorage";

const isNotValid = (value: string | null): value is string => [null, undefined, ''].includes(value)

export const Enhet = () => {
  const [valgtEnhet, setValgtEnhet] = useState<IEnhet | undefined>()
  const { enheter } = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)

  useEffect(() => {
    if (!enheter.length) {
      localStorage.removeItem(LocalStorageKeys.ENHET)
      return
    }

    const enhet = localStorage.getItem(LocalStorageKeys.ENHET)
    if (isNotValid(enhet)) {
      if (enheter.length) setValgtEnhet(enheter[0])
    } else {
      setValgtEnhet(JSON.parse(String(enhet)))
    }
  }, [])

  useEffect(() => {
    if (valgtEnhet === undefined && localStorage.getItem(LocalStorageKeys.ENHET) !== undefined) return
    localStorage.setItem(LocalStorageKeys.ENHET, JSON.stringify(valgtEnhet))
  }, [valgtEnhet])

  return (
    <EnhetWrap>
      <Select
        label={''}
        value={valgtEnhet?.enhetId ?? ''}
        key={'Enhet'}
        onChange={(e) => {
          setValgtEnhet(hentEnhet(enheter, e.target.value))
        }}
        hideLabel={true}
      >
        {enheter.length ? (
          enheter.map((enhet: IEnhet) => {
            return (
              <option key={enhet.enhetId} value={enhet.enhetId}>
                {`${enhet.enhetId} ${enhet.navn}`}
              </option>
            )
          })
        ) : (
          <option value="">Fant ingen enheter</option>
        )}
      </Select>
    </EnhetWrap>
  )
}

const EnhetWrap = styled.div`
  padding: 0.3em;
  min-width: 14rem;
`

const hentEnhet = (enheter: IEnhet[], enhetsId: string): IEnhet | undefined =>
  enheter.find((enhet) => enhet.enhetId === enhetsId)
