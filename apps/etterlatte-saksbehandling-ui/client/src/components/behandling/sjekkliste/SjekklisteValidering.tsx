import { ErrorSummary } from '@navikt/ds-react'
import styled from 'styled-components'

export const SjekklisteValidering = ({ errors }: { errors: String[] }) => {
  return (
    <>
      {errors.length > 0 && (
        <FeilOppsummering heading="For å gå videre må du rette opp følgende:">
          <ErrorSummary.Item href="#1">Feltet må hukses av for å ferdigstilles</ErrorSummary.Item>
        </FeilOppsummering>
      )}
    </>
  )
}

const FeilOppsummering = styled(ErrorSummary)`
  margin: 1em auto;
  width: 24em;
`
