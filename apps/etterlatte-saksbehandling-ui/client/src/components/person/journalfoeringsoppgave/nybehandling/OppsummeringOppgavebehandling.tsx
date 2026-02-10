import { Alert, Button, Detail, Heading, HStack, VStack } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { Navigate, useNavigate } from 'react-router-dom'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { SakType } from '~shared/types/sak'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import FullfoerOppgaveModal from '~components/person/journalfoeringsoppgave/nybehandling/FullfoerOppgaveModal'
import { gyldigBehandlingRequest } from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import React from 'react'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { formaterSakstype, formaterSpraak } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'

export default function OppsummeringOppgavebehandling() {
  const { journalpost, oppgave, nyBehandlingRequest, sakMedBehandlinger } = useJournalfoeringOppgave()

  const navigate = useNavigate()

  const tilbake = () => navigate('../', { relative: 'path' })

  if (!oppgave || !erOppgaveRedigerbar(oppgave?.status)) {
    return <Navigate to="../" relative="path" />
  }

  if (!journalpost || !nyBehandlingRequest || !sakMedBehandlinger || !gyldigBehandlingRequest(nyBehandlingRequest)) {
    return <Alert variant="error">Noe data i journalføringen eller behandlingen er feil</Alert>
  }

  const { spraak, mottattDato, persongalleri } = nyBehandlingRequest!

  if (!persongalleri || !mottattDato || !spraak) {
    return <Alert variant="error">Kunne ikke hente persongalleri, mottat dato eller språk</Alert>
  }

  return (
    <FormWrapper $column>
      <Heading size="medium" spacing>
        Opprett behandling fra oppgave
      </Heading>

      <VStack gap="space-4">
        <Info label="Saktype" tekst={formaterSakstype(nyBehandlingRequest.sakType!!)} />

        <Info label="Språk" tekst={formaterSpraak(spraak)} />
        <Info label="Mottatt dato" tekst={formaterDato(mottattDato)} />

        <Info label="Søker" tekst={persongalleri.soeker} />
        <Info label="Innsender" tekst={persongalleri.innsender || <Detail>Ikke oppgitt</Detail>} />

        {oppgave.sakType === SakType.BARNEPENSJON && persongalleri.gjenlevende?.length ? (
          persongalleri.gjenlevende?.map((gjenlevende) => (
            <Info key={gjenlevende} label="Gjenlevende" tekst={gjenlevende || ''} />
          ))
        ) : (
          <Info label="Gjenlevende" tekst={<Detail>Ikke oppgitt</Detail>} />
        )}

        {persongalleri.avdoed?.length ? (
          persongalleri.avdoed?.map((avdoed) => <Info key={avdoed} label="Avdød" tekst={avdoed} />)
        ) : (
          <Info label="Avdød" tekst={<Detail>Ikke oppgitt</Detail>} />
        )}

        {persongalleri.soesken?.map((soeskenEllerBarn) => (
          <Info
            key={soeskenEllerBarn}
            label={oppgave?.sakType === SakType.BARNEPENSJON ? 'Søsken' : 'Barn'}
            tekst={soeskenEllerBarn || ''}
          />
        )) || <Info label="Innsender" tekst={<Detail>Ikke oppgitt</Detail>} />}
      </VStack>

      {!persongalleri.avdoed?.length && (
        <Alert variant="warning" size="small">
          Avdød er påkrevd ved innvilgelse. Det anbefales derfor å legge til (hvis mulig) for å slippe oppdatering av
          persongalleriet på et senere tidspunkt.
        </Alert>
      )}

      <VStack gap="space-2">
        <HStack gap="space-4" justify="center">
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>

          <FullfoerOppgaveModal oppgave={oppgave} behandlingBehov={nyBehandlingRequest} />
        </HStack>
        <HStack justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </HStack>
      </VStack>
    </FormWrapper>
  )
}
