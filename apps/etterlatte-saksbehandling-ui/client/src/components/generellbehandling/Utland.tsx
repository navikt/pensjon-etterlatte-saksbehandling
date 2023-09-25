import { Dokumenter, Generellbehandling, Utland } from '~shared/types/Generellbehandling'
import { Content, ContentHeader, GridContainer, MainContent } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { Alert, Button, Checkbox, Heading, Link, ReadMore, Select, Table, Textarea, TextField } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { mapApiResult, mapAllApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterGenerellBehandling } from '~shared/api/generellbehandling'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentAlleLand, ILand } from '~shared/api/trygdetid'
import styled from 'styled-components'
import { PencilWritingIcon } from '@navikt/aksel-icons'

const TextFieldBegrunnelse = styled(Textarea).attrs({ size: 'medium' })`
  max-width: 50rem;
  margin: 2rem 0rem;
`

const DokumenterBrevFlex = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

const TableWidth = styled(Table)`
  max-width: 30rem;
`

function isUtland(
  utlandsBehandling: Generellbehandling
): utlandsBehandling is Generellbehandling & { innhold: Utland } {
  return utlandsBehandling.type === 'UTLAND'
}

const Utland = (props: { utlandsBehandling: Generellbehandling }) => {
  const { utlandsBehandling } = props

  const [rinanummer, setRinanummer] = useState<string>('')
  const [notater, setNotater] = useState<string>('')
  const [putOppdaterGenerellBehandlingStatus, putOppdaterGenerellBehandling] = useApiCall(oppdaterGenerellBehandling)

  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [valgtLandIsoKode, setValgtLandIsoKode] = useState<string>()
  const dokumentState = { P2100: false, P3000: false, P5000: false }
  if (isUtland(utlandsBehandling)) {
    //TODO: default dokumentState
  }
  const [dokumenter, setDokumenter] = useState<Dokumenter>(dokumentState)

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
    if (valgtLandIsoKode !== undefined) {
      const generellBehandling: Generellbehandling = {
        ...utlandsBehandling,
        innhold: {
          type: 'UTLAND',
          dokumenter: dokumenter,
          landIsoKode: valgtLandIsoKode,
          begrunnelse: notater,
          tilknyttetBehandling: notater,
        },
      }
      putOppdaterGenerellBehandling(generellBehandling)
    } else {
      //TODO: du må velge land...
    }
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
          </ContentHeader>
          <InnholdPadding>
            <div style={{ maxWidth: '20rem' }}>
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
                      <option value="" disabled={true}>
                        Velg land
                      </option>
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
              <TextField label="Saksnummer RINA" value={rinanummer} onChange={(e) => setRinanummer(e.target.value)} />
            </div>
            <DokumenterBrevFlex>
              <TableWidth>
                <Table.Header>
                  <Table.Row>
                    <Table.HeaderCell scope="col">Dokumenter fra RINA</Table.HeaderCell>
                    <Table.HeaderCell scope="col">Sendt</Table.HeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  <Table.Row>
                    <Table.HeaderCell scope="row">P2100</Table.HeaderCell>
                    <Table.DataCell>
                      <Checkbox
                        value={dokumenter.P2100}
                        onChange={() => setDokumenter({ ...dokumenter, P2100: !dokumenter.P2100 })}
                      ></Checkbox>
                    </Table.DataCell>
                  </Table.Row>
                  <Table.Row>
                    <Table.HeaderCell scope="row">P5000</Table.HeaderCell>
                    <Table.DataCell>
                      <Checkbox
                        value={dokumenter.P5000}
                        onChange={() => setDokumenter({ ...dokumenter, P5000: !dokumenter.P5000 })}
                      ></Checkbox>
                    </Table.DataCell>
                  </Table.Row>
                  <Table.Row>
                    <Table.HeaderCell scope="row">P3000</Table.HeaderCell>
                    <Table.DataCell>
                      <Checkbox
                        value={dokumenter.P3000}
                        onChange={() => setDokumenter({ ...dokumenter, P3000: !dokumenter.P3000 })}
                      ></Checkbox>
                    </Table.DataCell>
                  </Table.Row>
                </Table.Body>
              </TableWidth>
              <ReadMore defaultOpen={true} header="Sende brev til RINA">
                Når SED er sendt i RINA skal det sendes varling til bruker
                <div>
                  <Button icon={<PencilWritingIcon />}>Opprett nytt brev</Button>
                </div>
              </ReadMore>
            </DokumenterBrevFlex>
            <TextFieldBegrunnelse
              label="Notater(valgfri)"
              value={notater}
              onChange={(e) => setNotater(e.target.value)}
            />

            <Button onClick={() => oppaterGenerellbehandlingUtland()}>Lagre opplysninger</Button>
            {mapAllApiResult(
              putOppdaterGenerellBehandlingStatus,
              <Spinner visible={true} label="Oppdaterer generell behandling utland" />,
              null,
              () => (
                <ApiErrorAlert>Kunne ikke oppdatere generell behandling utland</ApiErrorAlert>
              ),
              () => (
                <Alert variant="success">Behandlingen er oppdatert</Alert>
              )
            )}
          </InnholdPadding>
        </Content>
      </MainContent>
    </GridContainer>
  )
}
export default Utland
