import { GenerellEndringshendelse, OppgaveDTO } from '~shared/types/oppgave'
import { isSuccess, mapSuccess, Result } from '~shared/api/apiUtils'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEndringer } from '~shared/api/oppgaver'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { BodyShort, Box, Detail } from '@navikt/ds-react'
import styled from 'styled-components'

export const OppgaveEndring = ({ oppgaveResult }: { oppgaveResult: Result<OppgaveDTO> }) => {
  const [endringerResult, hentOppgaveEndringer] = useApiCall(hentEndringer)

  useEffect(() => {
    if (isSuccess(oppgaveResult) && !!oppgaveResult.data) {
      hentOppgaveEndringer(oppgaveResult.data.id)
    }
  }, [oppgaveResult])

  return mapSuccess(endringerResult, (endringer) => (
    <Box padding="space-4">
      <EndringListe>
        {endringer.map((endring, index) => (
          <EndringLinje key={`endringlinje-${index}`} endring={endring} />
        ))}
      </EndringListe>
    </Box>
  ))
}

const EndringLinje = ({ endring }: { endring: GenerellEndringshendelse }) => {
  const { tidspunkt, saksbehandler, endringer } = endring

  return (
    <EndringElement>
      {endringer.map(({ tittel, beskrivelse }, index) => (
        <div key={`endring-element-${index}`}>
          <BodyShort size="small">
            <strong>{tittel}</strong>
          </BodyShort>
          {!!beskrivelse && <BodyShort size="small">{beskrivelse}</BodyShort>}
        </div>
      ))}
      {saksbehandler && <Detail>utført av {saksbehandler}</Detail>}
      <Detail>{formaterDatoMedKlokkeslett(tidspunkt)}</Detail>
    </EndringElement>
  )
}

export const EndringListe = styled.ul`
  list-style: none;
  padding: 0;
  margin-top: 1rem;
`

export const EndringElement = styled.li`
  padding-bottom: 1rem;
  border-left: 1px solid #abaaed;
  position: relative;
  padding-left: 20px;
  margin-left: 10px;

  &:first-child:before {
    background: var(--a-blue-200);
    border: none;
  }

  &:last-child {
    border: 0;
    padding-bottom: 0;
  }

  &:before {
    content: '';
    width: 15px;
    height: 15px;
    background: white;
    border: 3px solid var(--a-gray-600);
    border-radius: 50%;
    position: absolute;
    left: -8px;
    top: 0px;
  }
`
