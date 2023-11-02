import { Generellbehandling, Status, KravpakkeUtland, DokumentSendtMedDato } from '~shared/types/Generellbehandling'
import { Content, ContentHeader, GridContainer, MainContent } from '~shared/styled'
import { HeadingWrapper, InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
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
import React, { useContext, useEffect, useState } from 'react'
import { isFailure, isPending, isSuccess, mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import {
  attesterGenerellbehandling,
  oppdaterGenerellBehandling,
  sendTilAttesteringGenerellBehandling,
} from '~shared/api/generellbehandling'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentAlleLand, ILand, sorterLand } from '~shared/api/trygdetid'
import styled from 'styled-components'
import { ExternalLinkIcon, PencilWritingIcon, XMarkIcon } from '@navikt/aksel-icons'
import { opprettBrevForSak } from '~shared/api/brev'
import { ABlue500, AGray400 } from '@navikt/ds-tokens/dist/tokens'
import { ButtonGroup } from '~components/person/VurderHendelseModal'
import { ConfigContext } from '~clientConfig'
import { DatoVelger, formatDateToLocaleDateOrEmptyString } from '~shared/DatoVelger'
import { getGrunnlagsAvOpplysningstype } from '~shared/api/grunnlag'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { formaterNavn, IPdlPerson } from '~shared/types/Person'
import { KildePdl } from '~shared/types/kilde'
import { Info } from '~components/behandling/soeknadsoversikt/Info'

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

const KravpakkeUtland = (props: { utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null } }) => {
  const { utlandsBehandling } = props
  const innhold = utlandsBehandling.innhold
  const [putOppdaterGenerellBehandlingStatus, putOppdaterGenerellBehandling] = useApiCall(oppdaterGenerellBehandling)
  const [sendTilAttesteringStatus, sendTilAttestering] = useApiCall(sendTilAttesteringGenerellBehandling)
  const [attesterStatus, attesterFetch] = useApiCall(attesterGenerellbehandling)
  const [avdoedeStatus, avdoedeFetch] = useApiCall(getGrunnlagsAvOpplysningstype)
  const [avdoed, setAvdoed] = useState<Grunnlagsopplysning<IPdlPerson, KildePdl> | null>(null)

  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)
  const [rinanummer, setRinanummer] = useState<string>(innhold?.rinanummer ?? '')
  const [notater, setNotater] = useState<string>(innhold?.begrunnelse ?? '')
  const [valgtLandIsoKode, setValgtLandIsoKode] = useState<string>('')
  const [valgteLandIsoKode, setvalgteLandIsoKode] = useState<string[]>(innhold?.landIsoKode ?? [])
  const [landAlleredeValgt, setLandAlleredeValgt] = useState<boolean>(false)
  const defaultDokumentState: DokumentSendtMedDato[] = [{ dokumenttype: '', sendt: false, dato: '' }]

  const [dokumenter, setDokumenter] = useState<DokumentSendtMedDato[]>(
    utlandsBehandling.innhold?.dokumenter ?? defaultDokumentState
  )
  const [errorLand, setErrLand] = useState<boolean>(false)
  const [nyttBrevStatus, opprettBrev] = useApiCall(opprettBrevForSak)

  const configContext = useContext(ConfigContext)

  const opprettNyttBrevINyFane = () => {
    opprettBrev(Number(utlandsBehandling.sakId), (brev) => {
      window.open(`/person/${brev.soekerFnr}/sak/${brev.sakId}/brev/${brev.id}`, '_blank', 'noopener noreferrer')
    })
  }

  useEffect(() => {
    if (utlandsBehandling.tilknyttetBehandling) {
      avdoedeFetch(
        {
          sakId: utlandsBehandling.sakId,
          behandlingId: utlandsBehandling.tilknyttetBehandling,
          opplysningstype: 'AVDOED_PDL_V1',
        },
        (avdoed) => setAvdoed(avdoed)
      )
    }

    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(sorterLand(landliste))
    })
  }, [])

  const oppaterGenerellbehandlingUtland = () => {
    if (valgtLandIsoKode !== undefined) {
      setErrLand(false)
      const generellBehandling: Generellbehandling = {
        ...utlandsBehandling,
        innhold: {
          type: 'KRAVPAKKE_UTLAND',
          dokumenter: dokumenter,
          landIsoKode: valgteLandIsoKode,
          begrunnelse: notater,
          rinanummer: rinanummer,
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
        type: 'KRAVPAKKE_UTLAND',
        dokumenter: dokumenter,
        landIsoKode: valgteLandIsoKode,
        begrunnelse: notater,
        rinanummer: rinanummer,
      },
    }
    sendTilAttestering(generellBehandling, () => window.location.reload())
  }

  const redigerbar = utlandsBehandling.status === Status.OPPRETTET

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
              {utlandsBehandling.tilknyttetBehandling ? (
                <div>
                  {isSuccess(avdoedeStatus) && avdoed && (
                    <>
                      <h3>Informasjon om avdøde</h3>
                      <InfoWrapper>
                        <Info label="Navn" tekst={formaterNavn(avdoed.opplysning)} />
                        <Info label="Fødselsnummer" tekst={avdoed.opplysning.foedselsnummer} />
                      </InfoWrapper>
                    </>
                  )}
                  {isFailure(avdoedeStatus) && <ApiErrorAlert>Klarte ikke å hente informasjon om avdøed</ApiErrorAlert>}
                  {isPending(avdoedeStatus) && <Spinner visible={true} label="Henter opplysninger om avdøde" />}
                </div>
              ) : (
                <Alert variant="warning">
                  Denne utlandsbehandlingen er ikke tilknyttet en behandling. Vi kan derfor ikke hente avdoedes
                  informasjon
                </Alert>
              )}
              {mapApiResult(
                hentAlleLandRequest,
                <Spinner visible={true} label="Laster landliste" />,
                () => (
                  <ApiErrorAlert>Vi klarte ikke å hente landlisten</ApiErrorAlert>
                ),
                (landListe: ILand[]) => (
                  <>
                    <Heading size="medium" level="3" style={{ marginTop: '2rem' }}>
                      Kravpakke sendes til
                    </Heading>
                    <Select
                      readOnly={!redigerbar}
                      label="Land"
                      value={valgtLandIsoKode || ''}
                      onChange={(e) => {
                        setValgtLandIsoKode(e.target.value)
                        setErrLand(false)
                      }}
                      onBlur={() => setLandAlleredeValgt(false)}
                    >
                      <option value="" disabled={true}>
                        Velg land
                      </option>
                      {landListe.map((land) => (
                        <option key={land.isoLandkode} value={land.isoLandkode}>
                          {land.beskrivelse.tekst}
                        </option>
                      ))}
                    </Select>
                    {errorLand && <Alert variant="error">Du må velge land</Alert>}
                    <div style={{ margin: '1rem 0rem' }}>
                      <Button
                        disabled={!redigerbar}
                        onClick={() => {
                          setLandAlleredeValgt(false)
                          if (valgtLandIsoKode) {
                            const finnesAllerede = valgteLandIsoKode.includes(valgtLandIsoKode)
                            if (finnesAllerede) {
                              setLandAlleredeValgt(true)
                            } else {
                              const nyLandListe = valgteLandIsoKode.concat([valgtLandIsoKode])
                              setvalgteLandIsoKode(nyLandListe)
                            }
                          }
                        }}
                      >
                        Legg til land
                      </Button>
                      {landAlleredeValgt && <p>Landet er allerede valgt</p>}
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
                                if (redigerbar) {
                                  setLandAlleredeValgt(false)
                                  const nyLandliste = valgteLandIsoKode.filter(
                                    (isolandkode) => isolandkode !== landIsoKode
                                  )
                                  setvalgteLandIsoKode(nyLandliste)
                                }
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

              <LenkeMargin href={configContext['rinaUrl']} target="_blank" rel="noopener noreferrer">
                Gå til RINA for å opprette kravpakke til utlandet
                <ExternalLinkIcon fill={ABlue500} />
              </LenkeMargin>
              <TextField
                label="Saksnummer RINA"
                value={rinanummer}
                onChange={(e) => setRinanummer(e.target.value)}
                readOnly={!redigerbar}
              />
            </div>

            <StandardBreddeTabell>
              <Table.Header>
                <Table.Row>
                  <Table.HeaderCell scope="col">Dokumenttype(feks P2000)</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Sendt</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Dato sendt</Table.HeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {dokumenter.map((dokument, idx) => (
                  <Table.Row key={idx}>
                    <Table.DataCell>
                      {redigerbar ? (
                        <TextField
                          label={dokument.dokumenttype}
                          size="medium"
                          style={{ maxWidth: '16rem' }}
                          onChange={(e) => {
                            const oppdaterteDocType = dokumenter.map((doc, i) => {
                              if (idx === i) {
                                return { ...doc, dokumenttype: e.target.value }
                              }
                              return doc
                            })
                            setDokumenter(oppdaterteDocType)
                          }}
                        />
                      ) : (
                        <BodyShort>dokument.dokumenttype</BodyShort>
                      )}
                    </Table.DataCell>
                    <Table.DataCell>
                      <Checkbox
                        readOnly={!redigerbar}
                        checked={dokument.sendt}
                        onChange={(e) => {
                          const oppdaterteDocSendt = dokumenter.map((doc, i) => {
                            if (idx === i) {
                              return { ...doc, sendt: e.target.checked }
                            }
                            return doc
                          })
                          setDokumenter(oppdaterteDocSendt)
                        }}
                      >
                        <></>
                      </Checkbox>
                    </Table.DataCell>
                    <Table.DataCell>
                      <DatoVelger
                        disabled={!redigerbar}
                        label=""
                        value={dokument.dato ? new Date(dokument.dato) : undefined}
                        onChange={(date) => {
                          const oppdaterteDocDato = dokumenter.map((doc, i) => {
                            if (idx === i) {
                              return { ...doc, dato: formatDateToLocaleDateOrEmptyString(date) }
                            }
                            return doc
                          })
                          setDokumenter(oppdaterteDocDato)
                        }}
                      />
                    </Table.DataCell>
                  </Table.Row>
                ))}
              </Table.Body>
            </StandardBreddeTabell>
            {redigerbar && (
              <Button
                style={{ marginTop: '1.5rem' }}
                onClick={() => setDokumenter((dokumenter) => dokumenter.concat(defaultDokumentState))}
              >
                Legg til dokument
              </Button>
            )}
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
                  disabled={!redigerbar}
                  icon={<PencilWritingIcon />}
                  onClick={opprettNyttBrevINyFane}
                  loading={isPending(nyttBrevStatus)}
                  iconPosition="right"
                >
                  Opprett nytt brev
                </Button>
              </div>
            </div>
            <TextFieldBegrunnelse
              disabled={!redigerbar}
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
              {redigerbar && (
                <>
                  <Button
                    onClick={() => oppaterGenerellbehandlingUtland()}
                    loading={isPending(putOppdaterGenerellBehandlingStatus)}
                  >
                    Lagre opplysninger
                  </Button>
                  <Button
                    onClick={() => sendTilAttesteringWrapper()}
                    disabled={isFailure(sendTilAttesteringStatus)}
                    loading={isPending(sendTilAttesteringStatus)}
                  >
                    Send til attestering
                  </Button>
                  {isFailure(sendTilAttesteringStatus) && (
                    <ApiErrorAlert>Klarte ikke å attestere kravpakke utland. Prøv igjen senere.</ApiErrorAlert>
                  )}
                </>
              )}
              {utlandsBehandling.status === Status.FATTET && (
                <Button onClick={() => attesterFetch(utlandsBehandling)} loading={isPending(attesterStatus)}>
                  Attester
                </Button>
              )}
            </ButtonGroup>
          </Panel>
        </Content>
      </MainContent>
    </GridContainer>
  )
}
export default KravpakkeUtland
