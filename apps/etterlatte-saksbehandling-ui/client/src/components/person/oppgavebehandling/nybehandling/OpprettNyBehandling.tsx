import { useNyBehandling } from '~components/person/oppgavebehandling/useNyBehandling'
import { SakType } from '~shared/types/sak'
import PersongalleriBarnepensjon from '~components/person/oppgavebehandling/nybehandling/PersongalleriBarnepensjon'
import PersongalleriOmstillingsstoenad from '~components/person/oppgavebehandling/nybehandling/PersongalleriOmstillingsstoenad'
import { formaterSakstype } from '~utils/formattering'
import { Button, Heading, Tag } from '@navikt/ds-react'
import React from 'react'
import AvbrytOppgavebehandling from '~components/person/oppgavebehandling/AvbrytOppgavebehandling'
import { KnapperWrapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAppDispatch } from '~store/Store'
import { settBehandlingBehov } from '~store/reducers/NyBehandlingReducer'
import { Persongalleri } from '~shared/types/Person'
import { FormWrapper } from '~components/person/oppgavebehandling/styled'
import styled from 'styled-components'
import { gyldigPersongalleri } from '~components/person/oppgavebehandling/nybehandling/validator'

export default function OpprettNyBehandling() {
  const { oppgave, behandlingBehov } = useNyBehandling()
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  if (!oppgave) {
    return <Navigate to={'..'} relative={'path'} />
  }

  const { sakType } = oppgave!!

  const oppdater = (persongalleri: Persongalleri) => {
    dispatch(settBehandlingBehov({ ...behandlingBehov, persongalleri }))
  }

  const neste = () => navigate('../oppsummering', { relative: 'path' })
  const tilbake = () => navigate('..', { relative: 'path' })

  return (
    <FormWrapper column>
      <Heading size={'medium'} spacing>
        Opprett behandling{' '}
        <Tag variant={'success'} size={'medium'}>
          {formaterSakstype(sakType)}
        </Tag>
      </Heading>

      {sakType === SakType.OMSTILLINGSSTOENAD && (
        <PersongalleriOmstillingsstoenad
          persongalleri={behandlingBehov?.persongalleri}
          oppdaterPersongalleri={oppdater}
        />
      )}
      {sakType === SakType.BARNEPENSJON && (
        <PersongalleriBarnepensjon persongalleri={behandlingBehov?.persongalleri} oppdaterPersongalleri={oppdater} />
      )}

      <KnapperWrapper>
        <div>
          <Button variant="secondary" className="button" onClick={tilbake}>
            Tilbake
          </Button>

          <Button
            variant="primary"
            onClick={neste}
            className="button"
            disabled={!gyldigPersongalleri(sakType, behandlingBehov?.persongalleri)}
          >
            Neste
          </Button>
        </div>
        <AvbrytOppgavebehandling />
      </KnapperWrapper>
    </FormWrapper>
  )
}

export const InputList = styled.div`
  display: flex;
  flex-direction: column;
  align-items: start;
  gap: 1rem;
`

export const InputRow = styled.div`
  display: flex;
  align-items: flex-start;

  input {
    width: 20rem;
  }

  button {
    align-self: flex-end;
  }
`
