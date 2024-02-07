import { Alert, Button, Detail, Heading, Tag } from '@navikt/ds-react'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { useNavigate } from 'react-router-dom'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { SakType } from '~shared/types/sak'
import { formaterSakstype, formaterSpraak, formaterStringDato } from '~utils/formattering'
import { InfoList } from '~components/behandling/soeknadsoversikt/styled'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import FullfoerOppgaveModal from '~components/person/journalfoeringsoppgave/nybehandling/FullfoerOppgaveModal'
import { FlexRow } from '~shared/styled'
import { gyldigBehandlingRequest } from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import React from 'react'

export default function OppsummeringOppgavebehandling() {
  const { journalpost, nyBehandlingRequest, oppgave, sakMedBehandlinger } = useJournalfoeringOppgave()

  const navigate = useNavigate()

  const tilbake = () => navigate('../', { relative: 'path' })

  if (
    !journalpost ||
    !nyBehandlingRequest ||
    !oppgave ||
    !sakMedBehandlinger ||
    !gyldigBehandlingRequest(nyBehandlingRequest)
  ) {
    return null
  }

  const { spraak, mottattDato, persongalleri } = nyBehandlingRequest
  if (!spraak || !mottattDato || !persongalleri) {
    return null
  }

  return (
    <FormWrapper column>
      <Heading size="medium" spacing>
        Opprett behandling fra oppgave
      </Heading>

      <InfoList>
        <div>
          <Tag variant="success" size="medium">
            {formaterSakstype(oppgave.sakType)}
          </Tag>
        </div>

        <Info label="Språk" tekst={formaterSpraak(spraak)} />
        <Info label="Mottatt dato" tekst={formaterStringDato(mottattDato)} />

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
      </InfoList>

      {!persongalleri.avdoed?.length && (
        <Alert variant="warning" size="small">
          Avdød er påkrevd ved innvilgelse. Det anbefales derfor å legge til (hvis mulig) for å slippe oppdatering av
          persongalleriet på et senere tidspunkt.
        </Alert>
      )}

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>

          <FullfoerOppgaveModal oppgave={oppgave} behandlingBehov={nyBehandlingRequest} />
        </FlexRow>
        <FlexRow justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </FlexRow>
      </div>
    </FormWrapper>
  )
}
