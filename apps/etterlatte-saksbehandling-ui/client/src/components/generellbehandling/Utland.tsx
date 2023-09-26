import { Dokumenter, Generellbehandling, Utland } from '~shared/types/Generellbehandling'
import { Content, ContentHeader, GridContainer, MainContent } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import {
  Alert,
  BodyShort,
  Button,
  Checkbox,
  Heading,
  Link,
  Panel,
  Select,
  Table,
  Textarea,
  TextField,
} from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { mapApiResult, useApiCall, isPending, isFailure, isSuccess } from '~shared/hooks/useApiCall'
import { oppdaterGenerellBehandling } from '~shared/api/generellbehandling'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentAlleLand, ILand } from '~shared/api/trygdetid'
import styled from 'styled-components'
import { PencilWritingIcon } from '@navikt/aksel-icons'
import { opprettBrevForSak } from '~shared/api/brev'
import { useNavigate } from 'react-router-dom'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { ABlue500, AGray400 } from '@navikt/ds-tokens/dist/tokens'
import { ButtonGroup } from '~components/person/VurderHendelseModal'
import { XMarkIcon } from '@navikt/aksel-icons'

const TextFieldBegrunnelse = styled(Textarea).attrs({ size: 'medium' })`
  max-width: 40rem;
  margin: 2rem 0rem;
`

const StandardBreddeTabell = styled(Table)`
  margin-top: 2rem;
`

const LenkeMargin = styled(Link)`
  margin: 2rem 0rem 0.5rem 0;
`

const FlexOrder = styled.div`
  display: flex;
  max-width: 55rem;
  justify-content: flex-start;
  flex-wrap: wrap;
`

const Utland = (props: { utlandsBehandling: Generellbehandling & { innhold: Utland } }) => {
  const { utlandsBehandling } = props
  const navigate = useNavigate()
  const innhold = utlandsBehandling.innhold
  const [putOppdaterGenerellBehandlingStatus, putOppdaterGenerellBehandling] = useApiCall(oppdaterGenerellBehandling)
  const [sendTilAttesteringStatus, sendTilAttestering] = useApiCall(oppdaterGenerellBehandling)

  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)
  const [rinanummer, setRinanummer] = useState<string>(innhold?.rinanummer ?? '')
  const [notater, setNotater] = useState<string>(innhold?.begrunnelse ?? '')
  const [valgtLandIsoKode, setValgtLandIsoKode] = useState<string>('')
  const [valgteLandIsoKode, setvalgteLandIsoKode] = useState<string[]>(innhold?.landIsoKode ?? [])
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
    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(landliste)
    })
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
          landIsoKode: valgteLandIsoKode,
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

  const sendTilAttesteringWrapper = () => {
    const generellBehandling: Generellbehandling = {
      ...utlandsBehandling,
      innhold: {
        type: 'UTLAND',
        dokumenter: dokumenter,
        landIsoKode: valgteLandIsoKode,
        begrunnelse: notater,
        rinanummer: rinanummer,
        tilknyttetBehandling: notater,
      },
    }
    sendTilAttestering(generellBehandling)
  }

  return (
    <GridContainer>
      <MainContent style={{ whiteSpace: 'pre-wrap' }}>
        <Content style={{ maxWidth: '55rem', margin: 'auto' }}>
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
          </ContentHeader>
          <Panel>
            <div>
              {mapApiResult(
                hentAlleLandRequest,
                <Spinner visible={true} label="Laster landliste" />,
                () => (
                  <ApiErrorAlert>Vi klarte ikke å hente landlisten</ApiErrorAlert>
                ),
                (landListe: ILand[]) => (
                  <>
                    <Heading size="medium" level="3">
                      Kravpakke sendes til
                    </Heading>
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
                    <div style={{ margin: '1rem 0rem' }}>
                      <Button
                        onClick={() => {
                          if (valgtLandIsoKode) {
                            const finnesAllerede = valgteLandIsoKode.includes(valgtLandIsoKode)
                            if (!finnesAllerede) {
                              const nyLandListe = valgteLandIsoKode.concat([valgtLandIsoKode])
                              setvalgteLandIsoKode(nyLandListe)
                            }
                          }
                        }}
                      >
                        Legg til land
                      </Button>
                    </div>
                    {valgteLandIsoKode.length ? (
                      <Heading size="medium" level="3">
                        Valgte land
                      </Heading>
                    ) : null}
                    {isSuccess(hentAlleLandRequest) && valgteLandIsoKode && (
                      <FlexOrder>
                        {valgteLandIsoKode.map((landIsoKode) => {
                          const kodeverkLandMatch = alleLandKodeverk?.find(
                            (kodeverkLand) => kodeverkLand.isoLandkode === landIsoKode
                          )
                          return (
                            <BodyShort
                              style={{
                                borderRadius: '10px',
                                border: `2px solid ${AGray400}`,
                                cursor: 'pointer',
                                marginRight: '0.6rem',
                              }}
                              key={landIsoKode}
                              onClick={() => {
                                const nyLandliste = valgteLandIsoKode.filter(
                                  (isolandkode) => isolandkode !== landIsoKode
                                )
                                setvalgteLandIsoKode(nyLandliste)
                              }}
                            >
                              {kodeverkLandMatch?.beskrivelse.tekst ?? landIsoKode}
                              <XMarkIcon />
                            </BodyShort>
                          )
                        })}
                      </FlexOrder>
                    )}
                  </>
                )
              )}

              <LenkeMargin href="http://www.rina.com" target="_blank" rel="noopener noreferrer">
                Gå til RINA for å opprette kravpakke til utlandet
                <ExternalLinkIcon fill={ABlue500} />
              </LenkeMargin>
              <TextField label="Saksnummer RINA" value={rinanummer} onChange={(e) => setRinanummer(e.target.value)} />
            </div>
            <StandardBreddeTabell>
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
                      checked={dokumenter.p2100}
                      onChange={(e) => setDokumenter({ ...dokumenter, p2100: e.target.checked })}
                    >
                      <></>
                    </Checkbox>
                  </Table.DataCell>
                </Table.Row>
                <Table.Row>
                  <Table.HeaderCell scope="row">P5000</Table.HeaderCell>
                  <Table.DataCell>
                    <Checkbox
                      checked={dokumenter.p5000}
                      onChange={(e) => setDokumenter({ ...dokumenter, p5000: e.target.checked })}
                    >
                      <></>
                    </Checkbox>
                  </Table.DataCell>
                </Table.Row>
                <Table.Row>
                  <Table.HeaderCell scope="row">P3000</Table.HeaderCell>
                  <Table.DataCell>
                    <Checkbox
                      checked={dokumenter.p3000}
                      onChange={(e) => setDokumenter({ ...dokumenter, p3000: e.target.checked })}
                    >
                      <></>
                    </Checkbox>
                  </Table.DataCell>
                </Table.Row>
              </Table.Body>
            </StandardBreddeTabell>
            <div style={{ marginTop: '3.5rem', marginBottom: '3rem' }}>
              <Heading size="medium" level="3">
                Varsling til bruker
              </Heading>
              <p>
                Når nødvendige SED`er er sendt i RINA skal bruker varsles(om at krav er sendt). Opprett brev til bruker
                her. Tekstmal finner du her i tekstbiblioteket.
              </p>
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
            </div>
            <TextFieldBegrunnelse
              label="Notater(valgfri)"
              value={notater}
              onChange={(e) => setNotater(e.target.value)}
            />
            {isFailure(putOppdaterGenerellBehandlingStatus) && (
              <ApiErrorAlert>Kunne ikke oppdatere generell behandling utland</ApiErrorAlert>
            )}
            {isSuccess(putOppdaterGenerellBehandlingStatus) && (
              <Alert style={{ margin: '1rem', width: '20rem' }} variant="success">
                Behandlingen er oppdatert
              </Alert>
            )}
            <ButtonGroup>
              <Button
                onClick={() => oppaterGenerellbehandlingUtland()}
                loading={isPending(putOppdaterGenerellBehandlingStatus)}
              >
                Lagre opplysninger
              </Button>
              <Button onClick={() => sendTilAttesteringWrapper()} loading={isPending(sendTilAttesteringStatus)}>
                Send til attestering
              </Button>
            </ButtonGroup>
          </Panel>
        </Content>
      </MainContent>
    </GridContainer>
  )
}
export default Utland
