import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentTrygdetider, ITrygdetid, opprettTrygdetider } from '~shared/api/trygdetid'
import Spinner from '~shared/Spinner'
import { Alert, BodyShort, Box, Heading, Tabs, VStack } from '@navikt/ds-react'
import { TrygdeAvtale } from './avtaler/TrygdeAvtale'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { behandlingErIverksatt } from '~components/behandling/felles/utils'
import { VedtakResultat } from '~components/behandling/useVedtaksResultat'
import { EnkelPersonTrygdetid } from '~components/behandling/trygdetid/EnkelPersonTrygdetid'
import { BeregnetSamletTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetSamletTrygdetid'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { formaterNavn } from '~shared/types/Person'
import { Personopplysning } from '~shared/types/grunnlag'
import { skalViseTrygdeavtale } from '~components/behandling/trygdetid/utils'
import { TrygdetidMelding } from '~components/behandling/trygdetid/components/TrygdetidMelding'
import { hentAlleLand } from '~shared/api/behandling'
import { ILand, sorterLand } from '~utils/kodeverk'
import { ApiErrorAlert } from '~ErrorBoundary'

interface Props {
  redigerbar: boolean
  behandling: IDetaljertBehandling
  vedtaksresultat: VedtakResultat | null
  virkningstidspunktEtterNyRegelDato: boolean
}

const manglerTrygdetid = (trygdetider: ITrygdetid[], avdoede?: Personopplysning[]): boolean => {
  const avdoedIdenter = (avdoede || []).map((avdoed) => avdoed.opplysning.foedselsnummer)
  const trygdetidIdenter = trygdetider.map((trygdetid) => trygdetid.ident)

  return !avdoedIdenter.every((ident) => trygdetidIdenter.includes(ident))
}

export const Trygdetid = ({ redigerbar, behandling, vedtaksresultat, virkningstidspunktEtterNyRegelDato }: Props) => {
  const dispatch = useAppDispatch()
  const [hentTrygdetidRequest, fetchTrygdetid] = useApiCall(hentTrygdetider)
  const [opprettTrygdetidRequest, requestOpprettTrygdetid] = useApiCall(opprettTrygdetider)
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [trygdetider, setTrygdetider] = useState<ITrygdetid[]>([])
  const [landListe, setLandListe] = useState<ILand[]>()
  const [harPilotTrygdetid, setHarPilotTrygdetid] = useState<boolean>(false)
  const [behandlingsIdMangler, setBehandlingsIdMangler] = useState(false)
  const [trygdetidIdMangler, setTrygdetidIdMangler] = useState(false)
  const [trygdetidManglerVedAvslag, setTrygdetidManglerVedAvslag] = useState(false)

  const personopplysninger = usePersonopplysninger()

  const mapNavn = (fnr: string): string => {
    const opplysning = personopplysninger?.avdoede?.find(
      (personOpplysning) => personOpplysning.opplysning.foedselsnummer === fnr
    )?.opplysning

    if (!opplysning) {
      return fnr
    }

    return `${formaterNavn(opplysning)} (${fnr})`
  }

  const oppdaterTrygdetider = (trygdetid: ITrygdetid[]) => {
    setTrygdetider(trygdetid)
    dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
  }

  const fetchTrygdetider = (behandlingId: string) => {
    fetchTrygdetid(behandlingId, (trygdetider: ITrygdetid[]) => {
      if (
        trygdetider === null ||
        trygdetider.length == 0 ||
        manglerTrygdetid(trygdetider, personopplysninger?.avdoede)
      ) {
        if (behandlingErIverksatt(behandling.status)) {
          setHarPilotTrygdetid(true)
        } else if (redigerbar) {
          requestOpprettTrygdetid(behandling.id, (trygdetider: ITrygdetid[]) => {
            oppdaterTrygdetider(trygdetider)
          })
        } else if (vedtaksresultat === 'avslag') {
          setTrygdetidManglerVedAvslag(true)
        } else {
          setTrygdetidIdMangler(true)
          if (behandling.status !== IBehandlingStatus.AVBRUTT) {
            throw new Error('Kan ikke opprette trygdetid når readonly')
          }
        }
      } else {
        setTrygdetider(trygdetider)
      }
    })
  }

  useEffect(() => {
    if (!behandling?.id) {
      setBehandlingsIdMangler(true)
      throw new Error('Mangler behandlingsid')
    }

    fetchTrygdetider(behandling.id)
  }, [])

  useEffect(() => {
    fetchAlleLand(null, (landListe: ILand[]) => {
      setLandListe(sorterLand(landListe))
    })
  }, [])

  if (harPilotTrygdetid) {
    return (
      <TrygdetidMelding
        overskrift="Personen har fått 40 års trygdetid"
        beskrivelse="Denne søknaden ble satt automatisk til 40 års trygdetid"
      />
    )
  }

  if (trygdetidManglerVedAvslag) {
    return (
      <TrygdetidMelding
        overskrift="Informasjon om trygdetid mangler"
        beskrivelse="Dette er fordi avslag er gjort før trygdetidsbildet fantes"
      />
    )
  }

  return (
    <Box paddingInline="16" maxWidth="69rem">
      <VStack gap="12">
        {skalViseTrygdeavtale(behandling) && <TrygdeAvtale redigerbar={redigerbar} />}

        {landListe && (
          <>
            {trygdetider.length == 1 && (
              <EnkelPersonTrygdetid
                redigerbar={redigerbar}
                behandling={behandling}
                trygdetid={trygdetider[0]}
                landListe={landListe}
                virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato}
                fetchTrygdetider={fetchTrygdetider}
              />
            )}
            {trygdetider.length > 1 && (
              <>
                <Box maxWidth="fit-content">
                  <Alert variant="info">Det finnes flere avdøde, husk å oppdatere for alle</Alert>
                </Box>

                <Tabs defaultValue={trygdetider[0].ident}>
                  <Tabs.List>
                    {trygdetider.map((trygdetid) => (
                      <Tabs.Tab key={trygdetid.ident} value={trygdetid.ident} label={mapNavn(trygdetid.ident)} />
                    ))}
                  </Tabs.List>
                  {trygdetider.map((trygdetid) => (
                    <Tabs.Panel value={trygdetid.ident} key={trygdetid.ident}>
                      <Box paddingBlock="6 0">
                        <EnkelPersonTrygdetid
                          redigerbar={redigerbar}
                          behandling={behandling}
                          trygdetid={trygdetid}
                          landListe={landListe}
                          virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato}
                          fetchTrygdetider={fetchTrygdetider}
                        />
                      </Box>
                    </Tabs.Panel>
                  ))}
                </Tabs>

                <Box paddingBlock="0 8">
                  <Heading size="medium" level="2" spacing>
                    Oppsummering av trygdetid for flere avdøde
                  </Heading>
                  <VStack gap="8">
                    {trygdetider.map((trygdetid) => (
                      <div key={trygdetid.ident}>
                        {trygdetid.beregnetTrygdetid?.resultat ? (
                          <>
                            <Heading size="small" level="3" spacing>
                              Trygdetid for {mapNavn(trygdetid.ident)}
                            </Heading>
                            <BeregnetSamletTrygdetid beregnetTrygdetid={trygdetid.beregnetTrygdetid.resultat} />
                          </>
                        ) : (
                          <BodyShort>Trygdetid for {mapNavn(trygdetid.ident)} mangler</BodyShort>
                        )}
                      </div>
                    ))}
                  </VStack>
                </Box>
              </>
            )}
          </>
        )}

        <Spinner label="Henter trygdetid" visible={isPending(hentTrygdetidRequest) || isPending(hentAlleLandRequest)} />
        <Spinner label="Oppretter trygdetid" visible={isPending(opprettTrygdetidRequest)} />

        {isFailureHandler({
          apiResult: hentTrygdetidRequest,
          errorMessage: 'En feil har oppstått ved henting av trygdetid',
        })}
        {isFailureHandler({
          apiResult: opprettTrygdetidRequest,
          errorMessage: 'En feil har oppstått ved opprettelse av trygdetid',
        })}
        {isFailureHandler({
          apiResult: hentAlleLandRequest,
          errorMessage: 'Hent feil har oppstått ved henting av landliste',
        })}

        {behandlingsIdMangler && <ApiErrorAlert>Finner ikke behandling - ID mangler</ApiErrorAlert>}
        {trygdetidIdMangler && (
          <ApiErrorAlert>
            {behandling.status === IBehandlingStatus.AVBRUTT
              ? 'Behandlingen har ingen trygdetid'
              : 'Finner ikke trygdetid - ID mangler'}
          </ApiErrorAlert>
        )}
      </VStack>
    </Box>
  )
}
