import { ErrorSummary } from '@navikt/ds-react'
import styled from 'styled-components'
import { useSjekklisteValideringsfeil } from '~components/behandling/sjekkliste/useSjekkliste'
import React from 'react'

export const SjekklisteValideringErrorSummary = () => {
  const sjekklisteValideringsfeil = useSjekklisteValideringsfeil()

  return (
    <>
      {sjekklisteValideringsfeil.length > 0 && (
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
