import {
  DokumentSendtMedDato,
  Generellbehandling,
  generellbehandlingErRedigerbar,
  KravpakkeUtland,
} from '~shared/types/Generellbehandling'
import {
  Alert,
  BodyShort,
  Box,
  Button,
  Checkbox,
  Chips,
  Heading,
  HStack,
  Link,
  Select,
  Table,
  Textarea,
  TextField,
  VStack,
} from '@navikt/ds-react'
import React, { Fragment, useContext, useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { avbrytGenerellBehandling, oppdaterGenerellBehandling } from '~shared/api/generellbehandling'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'
import { ExternalLinkIcon, PencilWritingIcon, TrashIcon } from '@navikt/aksel-icons'
import { opprettBrevForSak } from '~shared/api/brev'
import { ConfigContext } from '~clientConfig'
import { DatoVelger } from '~shared/components/datoVelger/DatoVelger'
import { getGrunnlagsAvOpplysningstype } from '~shared/api/grunnlag'
import { formaterNavn } from '~shared/types/Person'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { hentSak } from '~shared/api/sak'
import { SendtilAttesteringModal } from '~components/generellbehandling/SendtilAttesteringModal'
import { GenerellbehandlingSidemeny } from '~components/generellbehandling/GenerellbehandlingSidemeny'
import { isPending, isPendingOrInitial, isSuccess, mapApiResult, mapResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { formatDateToLocaleDateOrEmptyString } from '~shared/components/datoVelger/datoVelgerUtils'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { hentAlleLand } from '~shared/api/behandling'
import { ILand, sorterLand } from '~utils/kodeverk'
import { NavigateFunction } from 'react-router-dom'

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

export const hentSakOgNavigerTilSaksoversikt = (sakId: number, navigate: NavigateFunction) => {
  hentSak(sakId)
    .then((sak) => {
      if (sak.ok) {
        navigate('/person', { state: { fnr: sak.data.ident } })
      } else {
        navigate('/')
      }
    })
    .catch(() => {
      navigate('/')
    })
}

const KravpakkeUtlandBehandling = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props
  const innhold = utlandsBehandling.innhold
  const [oppdaterGenerellBehandlingStatus, oppdaterGenerellBehandlingApi] = useApiCall(oppdaterGenerellBehandling)
  const [avbrytbehandlingStatus, avbrytBehandlingApi] = useApiCall(avbrytGenerellBehandling)
  const [avdoedeStatus, avdoedeFetch] = useApiCall(getGrunnlagsAvOpplysningstype)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)

  const [dokumentDropdown, setDokumentDropdown] = useState<string>('')

  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)
  const [rinanummer, setRinanummer] = useState<string>(innhold?.rinanummer ?? '')
  const [notater, setNotater] = useState<string>(innhold?.begrunnelse ?? '')
  const [valgtLandIsoKode, setValgtLandIsoKode] = useState<string>('')
  const [valgteLandIsoKode, setvalgteLandIsoKode] = useState<string[]>(innhold?.landIsoKode ?? [])
  const [landAlleredeValgt, setLandAlleredeValgt] = useState<boolean>(false)
  const defaultDokumentState: DokumentSendtMedDato[] = []

  const [dokumenter, setDokumenter] = useState<DokumentSendtMedDato[]>(
    utlandsBehandling.innhold?.dokumenter ?? defaultDokumentState
  )
  const [errorLand, setErrLand] = useState<boolean>(false)
  const [nyttBrevStatus, opprettBrev] = useApiCall(opprettBrevForSak)

  const configContext = useContext(ConfigContext)

  const [gjeldendeSakStatus, hentGjeldendeSak] = useApiCall(hentSak)

  const [redigerbar, setRedigerbar] = useState<boolean>(false)

  const opprettNyttBrevINyFane = () => {
    opprettBrev(Number(utlandsBehandling.sakId), (brev) => {
      window.open(`/person/sak/${brev.sakId}/brev/${brev.id}`, '_blank', 'noopener noreferrer')
    })
  }

  useEffect(() => {
    if (utlandsBehandling.tilknyttetBehandling) {
      avdoedeFetch({
        sakId: utlandsBehandling.sakId,
        behandlingId: utlandsBehandling.tilknyttetBehandling,
        opplysningstype: 'AVDOED_PDL_V1',
      })
    }

    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(sorterLand(landliste))
    })

    hentGjeldendeSak(utlandsBehandling.sakId, (result) => {
      setRedigerbar(
        generellbehandlingErRedigerbar(utlandsBehandling.status) &&
          enhetErSkrivbar(result.enhet, innloggetSaksbehandler.skriveEnheter)
      )
    })
  }, [])

  const generellBehandlingMedLocalState: Generellbehandling & { innhold: KravpakkeUtland } = {
    ...utlandsBehandling,
    innhold: {
      type: 'KRAVPAKKE_UTLAND',
      dokumenter: dokumenter,
      landIsoKode: valgteLandIsoKode,
      begrunnelse: notater,
      rinanummer: rinanummer,
    },
  }

  const avbrytBehandling = () => {
    avbrytBehandlingApi(utlandsBehandling)
  }

  const oppdaterGenerellbehandlingUtland = () => {
    if (valgtLandIsoKode !== undefined) {
      setErrLand(false)
      oppdaterGenerellBehandlingApi(generellBehandlingMedLocalState)
    } else {
      setErrLand(true)
    }
  }

  return (
    <HStack height="100%" minHeight="100vh" wrap={false}>
      <Box width="100%" style={{ whiteSpace: 'pre-wrap' }}>
        <div style={{ maxWidth: '55rem', margin: 'auto' }}>
          <Box paddingInline="space-16" paddingBlock="space-16 space-4">
            <Heading spacing size="large" level="1">
              Kravpakke til utland
            </Heading>
            <p>
              Det skal opprettes P_BUC_02 og sendes kravpakke som inneholder ulike SED`er til utland i RINA. Dokumenter
              her for hvilke SED`er som blir sendt, og fyll inn nødvendig informasjon. Bruk notatfeltet ved behov for
              utfyllende informasjon.
            </p>
          </Box>
          <Box padding="space-4">
            <div>
              {utlandsBehandling.tilknyttetBehandling ? (
                <div>
                  {mapResult(avdoedeStatus, {
                    pending: <Spinner label="Henter opplysninger om avdøde" />,
                    error: (error) => (
                      <ApiErrorAlert>Klarte ikke å hente informasjon om avdød: {error.detail}</ApiErrorAlert>
                    ),
                    success: (avdoed) => (
                      <>
                        <h3>Informasjon om avdøde</h3>
                        <VStack gap="space-4">
                          <Info label="Navn" tekst={formaterNavn(avdoed.opplysning)} />
                          <Info label="Fødselsnummer" tekst={avdoed.opplysning.foedselsnummer} />
                        </VStack>
                      </>
                    ),
                  })}
                </div>
              ) : (
                <Alert variant="warning">
                  Denne utlandsbehandlingen er ikke tilknyttet en behandling. Vi kan derfor ikke hente avdoedes
                  informasjon
                </Alert>
              )}
              {mapApiResult(
                hentAlleLandRequest,
                <Spinner label="Laster landliste" />,
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
                    {valgteLandIsoKode && (
                      <Chips>
                        {valgteLandIsoKode.map((landIsoKode) => {
                          const kodeverkLandMatch = alleLandKodeverk?.find(
                            (kodeverkLand) => kodeverkLand.isoLandkode === landIsoKode
                          )
                          return (
                            <Fragment key={landIsoKode}>
                              {redigerbar ? (
                                <Chips.Removable
                                  data-color="accent"
                                  style={{ cursor: 'pointer' }}
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
                                </Chips.Removable>
                              ) : (
                                <Chips.Toggle data-color="neutral">
                                  {kodeverkLandMatch?.beskrivelse.tekst ?? landIsoKode}
                                </Chips.Toggle>
                              )}
                            </Fragment>
                          )
                        })}
                      </Chips>
                    )}
                  </>
                )
              )}

              <LenkeMargin href={configContext['rinaUrl']} target="_blank" rel="noopener noreferrer">
                Gå til RINA for å opprette kravpakke til utlandet
                <ExternalLinkIcon fill="var(--a-blue-500)" aria-hidden />
              </LenkeMargin>
              <TextField
                label="Saksnummer RINA"
                value={rinanummer}
                onChange={(e) => setRinanummer(e.target.value)}
                readOnly={!redigerbar}
              />
            </div>
            <div style={{ marginTop: '2rem' }}>
              <Select
                label="Hvile dokumenter vil du legge til?"
                value={dokumentDropdown}
                onChange={(e) => setDokumentDropdown(e.target.value)}
              >
                <option value="" disabled={true}>
                  Velg dokument
                </option>
                <option value="P2100">P2100</option>
                <option value="P3000">P3000</option>
                <option value="P4000">P4000</option>
                <option value="P5000">P5000</option>
                <option value="P6000">P6000</option>
                <option value="P8000">P8000</option>
                <option value="velg dokument">Annet</option>
              </Select>
              {redigerbar && (
                <Button
                  style={{ marginTop: '0.5rem' }}
                  onClick={() => {
                    if (dokumentDropdown) {
                      const nyttDokument: DokumentSendtMedDato = {
                        dokumenttype: dokumentDropdown,
                        sendt: false,
                        dato: '',
                      }
                      setDokumenter((prev) => prev.concat([nyttDokument]))
                    }
                  }}
                >
                  Legg til valgt dokument
                </Button>
              )}
            </div>
            <StandardBreddeTabell>
              <Table.Header>
                <Table.Row>
                  <Table.HeaderCell scope="col">Dokumenttype(feks P2000)</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Sendt</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Dato sendt</Table.HeaderCell>
                  <Table.HeaderCell scope="col" />
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {dokumenter.map((dokument, idx) => {
                  const fjernDokument = () => {
                    setDokumenter((dokumenter) => dokumenter.filter((_, i) => idx !== i))
                  }
                  return (
                    <Table.Row key={idx}>
                      <Table.DataCell>
                        {redigerbar ? (
                          <TextField
                            label=""
                            value={dokument.dokumenttype}
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
                          <BodyShort>{dokument.dokumenttype}</BodyShort>
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
                      <Table.DataCell>
                        <Button
                          disabled={!redigerbar}
                          variant="tertiary"
                          icon={<TrashIcon aria-hidden />}
                          onClick={() => fjernDokument()}
                          style={{ marginLeft: '5rem' }}
                        >
                          Slett dokument
                        </Button>
                      </Table.DataCell>
                    </Table.Row>
                  )
                })}
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
                  disabled={!redigerbar}
                  icon={<PencilWritingIcon aria-hidden />}
                  onClick={opprettNyttBrevINyFane}
                  loading={isPending(nyttBrevStatus)}
                  iconPosition="right"
                >
                  Åpne brev i ny fane
                </Button>
              </div>
            </div>
            <TextFieldBegrunnelse
              disabled={!redigerbar}
              label="Notater(valgfri)"
              value={notater}
              onChange={(e) => setNotater(e.target.value)}
            />
            {isFailureHandler({
              apiResult: oppdaterGenerellBehandlingStatus,
              errorMessage: 'Kunne ikke oppdatere generell behandling utland',
            })}
            {isSuccess(oppdaterGenerellBehandlingStatus || avbrytbehandlingStatus) && (
              <Alert style={{ margin: '1rem', width: '20rem' }} variant="success">
                Behandlingen er oppdatert
              </Alert>
            )}

            <Spinner visible={isPendingOrInitial(gjeldendeSakStatus)} label="Henter opplysninger om sak" />

            {isFailureHandler({
              errorMessage: 'Vi klarte ikke å hente gjeldende sak',
              apiResult: gjeldendeSakStatus,
            })}
            {isFailureHandler({
              apiResult: avbrytbehandlingStatus,
              errorMessage: 'Kunne ikke avbryte generell behandling utland',
            })}
            <HStack gap="space-2" justify="end">
              {redigerbar && (
                <>
                  <Button onClick={() => avbrytBehandling()} loading={isPending(avbrytbehandlingStatus)}>
                    Avbryt
                  </Button>
                  <Button
                    onClick={() => oppdaterGenerellbehandlingUtland()}
                    loading={isPending(oppdaterGenerellBehandlingStatus)}
                  >
                    Lagre opplysninger
                  </Button>
                  <SendtilAttesteringModal utlandsBehandling={generellBehandlingMedLocalState} />
                </>
              )}
            </HStack>
          </Box>
        </div>
      </Box>
      <GenerellbehandlingSidemeny utlandsBehandling={generellBehandlingMedLocalState} />
    </HStack>
  )
}
export default KravpakkeUtlandBehandling
