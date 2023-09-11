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
import { GYLDIG_FNR } from '~utils/fnr'
import { FormWrapper } from '~components/person/oppgavebehandling/styled'
import styled from 'styled-components'

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

  const kanGaaVidere = () => {
    const persongalleri = behandlingBehov?.persongalleri
    if (!persongalleri) {
      return false
    }

    const avdoede = persongalleri.avdoed?.filter((fnr) => GYLDIG_FNR(fnr)) || []
    const gjenlevende = persongalleri.gjenlevende?.filter((fnr) => GYLDIG_FNR(fnr)) || []
    const antallGjenlevOgAvdoed = avdoede.length + gjenlevende.length

    return (
      !!persongalleri &&
      GYLDIG_FNR(persongalleri.soeker) &&
      GYLDIG_FNR(persongalleri.innsender) &&
      antallGjenlevOgAvdoed == 2
    )
  }

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
          <Button variant="secondary" size="medium" onClick={tilbake}>
            Tilbake
          </Button>

          <Button variant="primary" size="medium" className="button" onClick={neste} disabled={!kanGaaVidere()}>
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
