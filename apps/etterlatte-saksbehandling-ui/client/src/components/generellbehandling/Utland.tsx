import { Generellbehandling } from '~shared/types/Generellbehandling'
import { Content, ContentHeader, GridContainer, MainContent } from '~shared/styled'
import { HeadingWrapper, Innhold } from '~components/behandling/soeknadsoversikt/styled'
import { Alert, Button, Heading, Link, Select, TextField } from '@navikt/ds-react'
import { SoeknadsdatoDate } from '~components/generellbehandling/SoeknadsdatoDate'
import React, { useEffect, useState } from 'react'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterGenerellBehandling } from '~shared/api/generellbehandling'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentAlleLand, ILand } from '~shared/api/trygdetid'

const Utland = (props: { utlandsBehandling: Generellbehandling }) => {
  const { utlandsBehandling } = props
  const [sed, setSed] = useState<string>('')
  const [rinanummer, setRinanummer] = useState<string>('')
  const [valgtBehandling, setvalgtBehandling] = useState<string>('')
  const [postOppdaterGenerellBehandlingStatus, postOppdaterGenerellBehandling] = useApiCall(oppdaterGenerellBehandling)

  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [valgtLandIsoKode, setValgtLandIsoKode] = useState<string>()

  useEffect(() => {
    fetchAlleLand(null)
  }, [])

  const sorterLand = (landListe: ILand[]): ILand[] => {
    landListe.sort((a: ILand, b: ILand) => {
      if (a.beskrivelse.tekst > b.beskrivelse.tekst) {
        return 1
      }
      return -1
    })
    return landListe
  }
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
                Kravpakke til utland
              </Heading>
              <p>
                Det skal opprettes P_BUC_02 og sendes kravpakke som inneholder ulike SED`er til utland i RINA.
                Dokumenter her for hvilke SED`er som blir sendt, og fyll inn nødvendig informasjon. Bruk notatfeltet ved
                behov for utfyllende informasjon.
              </p>
            </HeadingWrapper>
            <Link href="http://www.rina.com" target="_blank" rel="noopener noreferrer">
              Gå til RINA for å opprette kravpakke til utlandet
            </Link>
            <SoeknadsdatoDate mottattDato={utlandsBehandling.opprettet} />
          </ContentHeader>
          <Innhold>
            {mapApiResult(
              hentAlleLandRequest,
              <Spinner visible={true} label="Laster landliste" />,
              () => (
                <ApiErrorAlert>Vi klarte ikke å hente landlisten</ApiErrorAlert>
              ),
              (landListe: ILand[]) => (
                <>
                  <Select
                    label="Land"
                    value={valgtLandIsoKode || ''}
                    onChange={(e) => setValgtLandIsoKode(e.target.value)}
                  >
                    {sorterLand(landListe).map((land) => (
                      <option key={land.isoLandkode} value={land.isoLandkode}>
                        {land.beskrivelse.tekst}
                      </option>
                    ))}
                  </Select>
                </>
              )
            )}
            <p>Utsendelse av SED</p>
            <TextField label="Vennligst fyll ut SED" value={sed} onChange={(e) => setSed(e.target.value)} />
            <TextField label="Saksnummer RINA" value={rinanummer} onChange={(e) => setRinanummer(e.target.value)} />
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
