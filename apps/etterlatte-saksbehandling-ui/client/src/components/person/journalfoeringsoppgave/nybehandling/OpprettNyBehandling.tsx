import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { SakType } from '~shared/types/sak'
import PersongalleriBarnepensjon from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriBarnepensjon'
import PersongalleriOmstillingsstoenad from '~components/person/journalfoeringsoppgave/nybehandling/PersongalleriOmstillingsstoenad'
import { formaterSakstype } from '~utils/formattering'
import { Button, Heading, Tag } from '@navikt/ds-react'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { KnapperWrapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Navigate, useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import styled from 'styled-components'
import { gyldigPersongalleri } from '~components/person/journalfoeringsoppgave/nybehandling/validator'

export default function OpprettNyBehandling() {
  const { oppgave, behandlingBehov } = useJournalfoeringOppgave()
  const navigate = useNavigate()

  if (!oppgave) {
    return <Navigate to={'../kontroll'} relative={'path'} />
  }

  const { sakType } = oppgave!!

  const neste = () => navigate('../oppsummering', { relative: 'path' })
  const tilbake = () => navigate('../kontroll   ', { relative: 'path' })

  return (
    <FormWrapper column>
      <Heading size={'medium'} spacing>
        Opprett behandling{' '}
        <Tag variant={'success'} size={'medium'}>
          {formaterSakstype(sakType)}
        </Tag>
      </Heading>

      {sakType === SakType.OMSTILLINGSSTOENAD && <PersongalleriOmstillingsstoenad />}
      {sakType === SakType.BARNEPENSJON && <PersongalleriBarnepensjon />}

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
        <AvbrytBehandleJournalfoeringOppgave />
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
