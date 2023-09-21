import { Generellbehandling } from '~shared/types/Generellbehandling'
import { Content, ContentHeader, GridContainer, MainContent } from '~shared/styled'
import { HeadingWrapper, Innhold } from '~components/behandling/soeknadsoversikt/styled'
import { Alert, Button, Heading, TextField } from '@navikt/ds-react'
import { SoeknadsdatoDate } from '~components/generellbehandling/SoeknadsdatoDate'
import React, { useState } from 'react'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterGenerellBehandling } from '~shared/api/generellbehandling'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

const Utland = (props: { utlandsBehandling: Generellbehandling }) => {
  const { utlandsBehandling } = props
  const [sed, setSed] = useState<string>('')
  const [rinanummer, setRinanummer] = useState<string>('')
  const [valgtBehandling, setvalgtBehandling] = useState<string>('')
  const [postOppdaterGenerellBehandlingStatus, postOppdaterGenerellBehandling] = useApiCall(oppdaterGenerellBehandling)

  const oppaterGenerellbehandlingUtland = () => {
    const generellBehandling: Generellbehandling = {
      ...utlandsBehandling,
      innhold: {
        type: 'UTLAND',
        sed: sed,
        tilknyttetBehandling: valgtBehandling,
      },
    }
    postOppdaterGenerellBehandling(generellBehandling)
  }
  return (
    <GridContainer>
      <MainContent>
        <Content>
          <ContentHeader>
            <HeadingWrapper>
              <Heading spacing size="large" level="1">
                Generell behandling utland
              </Heading>
            </HeadingWrapper>
            <SoeknadsdatoDate mottattDato={utlandsBehandling.opprettet} />
          </ContentHeader>
          <Innhold>
            <TextField label="Vennligst fyll ut SED" value={sed} onChange={(e) => setSed(e.target.value)} />
            <TextField label="Rina nummer" value={rinanummer} onChange={(e) => setRinanummer(e.target.value)} />
            <TextField
              label="Velg behandling"
              value={valgtBehandling}
              onChange={(e) => setvalgtBehandling(e.target.value)}
            />

            <Button onClick={() => oppaterGenerellbehandlingUtland()}>Lagre opplysninger</Button>
            {mapApiResult(
              postOppdaterGenerellBehandlingStatus,
              <Spinner visible={true} label="Oppdaterer generell behandling utland" />,
              () => (
                <ApiErrorAlert>Kunne ikke oppdatere generell behandling utland</ApiErrorAlert>
              ),
              () => (
                <Alert variant="success">Behandlingen er oppdatert</Alert>
              )
            )}
          </Innhold>
        </Content>
      </MainContent>
    </GridContainer>
  )
}
export default Utland
