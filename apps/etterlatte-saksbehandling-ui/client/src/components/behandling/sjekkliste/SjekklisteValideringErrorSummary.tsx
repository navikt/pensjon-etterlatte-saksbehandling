import { ErrorSummary } from '@navikt/ds-react'
import styled from 'styled-components'
import { useSjekklisteValideringsfeil } from '~components/behandling/sjekkliste/useSjekkliste'
import React, { useEffect } from 'react'
import { visSjekkliste } from '~store/reducers/BehandlingSidemenyReducer'
import { useAppDispatch } from '~store/Store'

export const SjekklisteValideringErrorSummary = () => {
  const sjekklisteValideringsfeil = useSjekklisteValideringsfeil()
  const dispatch = useAppDispatch()
  const harValideringsfeil = sjekklisteValideringsfeil.length > 0

  useEffect(() => {
    if (harValideringsfeil) {
      dispatch(visSjekkliste())
    }
  }, [harValideringsfeil])

  return (
    <>
      {harValideringsfeil && (
        <FeilOppsummering heading="For å gå videre må du rette opp følgende:">
          {sjekklisteValideringsfeil.map((item, index) => (
            <ErrorSummary.Item key={index}>{item}</ErrorSummary.Item>
          ))}
        </FeilOppsummering>
      )}
    </>
  )
}

const FeilOppsummering = styled(ErrorSummary)`
  margin: 1em auto;
  width: 24em;
`
