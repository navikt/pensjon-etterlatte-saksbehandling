import { Dokumenter, Generellbehandling, Utland } from '~shared/types/Generellbehandling'
import { Content, ContentHeader, GridContainer, MainContent } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { Alert, Button, Checkbox, Heading, Link, ReadMore, Select, Table, Textarea, TextField } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { mapApiResult, mapAllApiResult, useApiCall, isPending } from '~shared/hooks/useApiCall'
import { oppdaterGenerellBehandling } from '~shared/api/generellbehandling'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentAlleLand, ILand } from '~shared/api/trygdetid'
import styled from 'styled-components'
import { PencilWritingIcon } from '@navikt/aksel-icons'
import { opprettBrevForSak } from '~shared/api/brev'
import { useNavigate } from 'react-router-dom'

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
  const navigate = useNavigate()

  if (isUtland(utlandsBehandling)) {
    const innhold = utlandsBehandling.innhold
    const [putOppdaterGenerellBehandlingStatus, putOppdaterGenerellBehandling] = useApiCall(oppdaterGenerellBehandling)

    const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
    const [rinanummer, setRinanummer] = useState<string>(innhold?.rinanummer ?? '')
    const [notater, setNotater] = useState<string>(innhold?.begrunnelse ?? '')
    const [valgtLandIsoKode, setValgtLandIsoKode] = useState<string>(innhold?.landIsoKode ?? '')

    const defaultDokumentState: Dokumenter = { p2100: false, p3000: false, p5000: false }
    const [dokumenter, setDokumenter] = useState<Dokumenter>(innhold?.dokumenter ?? defaultDokumentState)

    const [errorLand, setErrLand] = useState<boolean>(false)
    const [nyttBrevStatus, opprettBrev] = useApiCall(opprettBrevForSak)
    const opprettNyttBrevOgRedirect = () => {
      opprettBrev(Number(utlandsBehandling.sakId), (brev) => {
        navigate(`/person/${brev.soekerFnr}/sak/${brev.sakId}/brev/${brev.id}`)
      })
    }

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
        setErrLand(false)
        const generellBehandling: Generellbehandling = {
          ...utlandsBehandling,
          innhold: {
            type: 'UTLAND',
            dokumenter: dokumenter,
            landIsoKode: valgtLandIsoKode,
            begrunnelse: notater,
            rinanummer: rinanummer,
            tilknyttetBehandling: notater,
          },
        }
        putOppdaterGenerellBehandling(generellBehandling)
      } else {
        setErrLand(true)
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
                  Dokumenter her for hvilke SED`er som blir sendt, og fyll inn nødvendig informasjon. Bruk notatfeltet
                  ved behov for utfyllende informasjon.
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
                        onChange={(e) => {
                          setValgtLandIsoKode(e.target.value)
                          setErrLand(false)
                        }}
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
                      {errorLand && <Alert variant="error">Du må velge land</Alert>}
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
                          value={dokumenter.p2100}
                          onChange={(e) => setDokumenter({ ...dokumenter, p2100: !!e.target.value })}
                        ></Checkbox>
                      </Table.DataCell>
                    </Table.Row>
                    <Table.Row>
                      <Table.HeaderCell scope="row">P5000</Table.HeaderCell>
                      <Table.DataCell>
                        <Checkbox
                          value={dokumenter.p5000}
                          onChange={(e) => setDokumenter({ ...dokumenter, p5000: !!e.target.value })}
                        ></Checkbox>
                      </Table.DataCell>
                    </Table.Row>
                    <Table.Row>
                      <Table.HeaderCell scope="row">P3000</Table.HeaderCell>
                      <Table.DataCell>
                        <Checkbox
                          value={dokumenter.p3000}
                          onChange={(e) => setDokumenter({ ...dokumenter, p3000: !!e.target.value })}
                        ></Checkbox>
                      </Table.DataCell>
                    </Table.Row>
                  </Table.Body>
                </TableWidth>
                <ReadMore defaultOpen={true} header="Sende brev til RINA">
                  Når SED er sendt i RINA skal det sendes varling til bruker
                  <div>
                    <Button
                      icon={<PencilWritingIcon />}
                      onClick={opprettNyttBrevOgRedirect}
                      loading={isPending(nyttBrevStatus)}
                      iconPosition="right"
                    >
                      Opprett nytt brev
                    </Button>
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
}
export default Utland
