import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { Navigate, useNavigate } from 'react-router-dom'
import { Alert, Button, Heading, Tag } from '@navikt/ds-react'
import { formaterSakstype, formaterStringDato } from '~utils/formattering'
import { InfoList } from '~components/behandling/soeknadsoversikt/styled'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { FlexRow } from '~shared/styled'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import FullfoerKlageModal from '~components/person/journalfoeringsoppgave/oppretteklage/FullfoerKlageModal'
import { NyKlageRequestUtfylling } from '~shared/types/Klage'

export function opprettKlageErUtfylt(
  utfylling?: Partial<NyKlageRequestUtfylling>
): utfylling is NyKlageRequestUtfylling {
  if (!utfylling) {
    return false
  }
  return !!utfylling.mottattDato
}

export default function OppsummeringKlagebehandling() {
  const { oppgave, nyKlageRequest, journalpost } = useJournalfoeringOppgave()
  const navigate = useNavigate()

  if (!oppgave || !nyKlageRequest || !journalpost) {
    return <Navigate to="../" relative="path" />
  }

  if (!opprettKlageErUtfylt(nyKlageRequest)) {
    return null
  }

  const { mottattDato } = nyKlageRequest

  const tilbake = () => navigate('../', { relative: 'path' })

  return (
    <FormWrapper column>
      <Heading size="medium" spacing>
        Opprett klage fra oppgave{' '}
        <Tag variant="success" size="medium">
          {formaterSakstype(oppgave.sakType)}
        </Tag>
      </Heading>

      <InfoList>
        <Info label="Klage framsatt dato" tekst={formaterStringDato(mottattDato)} />

        <Alert variant="warning">
          {/* TODO: støtte for at vi bare sender ut et strukturert kvitteringsbrev når klagen opprettes */}
          Etter at klagebehandlingen er opprettet må du sende ut et kvitteringsbrev til den som har sendt inn klagen
          manuelt.
        </Alert>
      </InfoList>
      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>
          <FullfoerKlageModal oppgave={oppgave} klageRequest={nyKlageRequest} journalpost={journalpost} />
        </FlexRow>

        <FlexRow justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </FlexRow>
      </div>
    </FormWrapper>
  )
}
