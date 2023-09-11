import { Button, Detail, Heading, Tag } from '@navikt/ds-react'
import { useNyBehandling } from '~components/person/oppgavebehandling/useNyBehandling'
import AvbrytOppgavebehandling from '~components/person/oppgavebehandling/AvbrytOppgavebehandling'
import { KnapperWrapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { SakType } from '~shared/types/sak'
import { formaterSakstype } from '~utils/formattering'
import { InfoList } from '~components/behandling/soeknadsoversikt/styled'
import { FormWrapper } from '~components/person/oppgavebehandling/styled'
import FullfoerOppgaveModal from '~components/person/oppgavebehandling/oppsummering/FullfoerOppgaveModal'

export default function OppsummeringOppgavebehandling() {
  const { behandlingBehov, oppgave } = useNyBehandling()

  const navigate = useNavigate()

  const tilbake = () => navigate('..', { relative: 'path' })

  if (!behandlingBehov || !oppgave) {
    navigate('..', { relative: 'route' })
    return null
  }

  return (
    <FormWrapper column>
      <Heading size={'medium'} spacing>
        Opprett behandling fra oppgave
      </Heading>

      <InfoList>
        <div>
          <Tag variant={'success'} size={'medium'}>
            {formaterSakstype(oppgave!!.sakType)}
          </Tag>
        </div>

        <Info label={'Søker'} tekst={behandlingBehov?.persongalleri?.soeker || '-'} />
        <Info label={'Innsender'} tekst={behandlingBehov?.persongalleri?.innsender || '-'} />

        {oppgave!!.sakType === SakType.BARNEPENSJON &&
          behandlingBehov?.persongalleri?.gjenlevende?.map((gjenlevende) => (
            <Info key={gjenlevende} label={'Gjenlevende'} tekst={gjenlevende || ''} />
          ))}

        {behandlingBehov?.persongalleri?.avdoed?.map((avdoed) => (
          <Info key={avdoed} label={'Avdød'} tekst={avdoed || ''} />
        ))}

        {!behandlingBehov?.persongalleri?.soesken?.length && <Detail>Ingen barn/søsken oppgitt</Detail>}
        {behandlingBehov?.persongalleri?.soesken?.map((soeskenEllerBarn) =>
          oppgave!!.sakType === SakType.BARNEPENSJON ? (
            <Info key={soeskenEllerBarn} label={'Søsken'} tekst={soeskenEllerBarn || ''} />
          ) : (
            <Info key={soeskenEllerBarn} label={'Barn'} tekst={soeskenEllerBarn || ''} />
          )
        )}
      </InfoList>

      <KnapperWrapper>
        <div>
          <Button variant="secondary" size="medium" onClick={tilbake}>
            Tilbake
          </Button>

          <FullfoerOppgaveModal oppgave={oppgave!!} behandlingBehov={behandlingBehov!!} />
        </div>
        <AvbrytOppgavebehandling />
      </KnapperWrapper>
    </FormWrapper>
  )
}
