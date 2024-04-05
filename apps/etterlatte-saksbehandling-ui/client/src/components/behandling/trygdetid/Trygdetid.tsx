import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleLand, hentTrygdetider, ILand, ITrygdetid, opprettTrygdetider, sorterLand } from '~shared/api/trygdetid'
import Spinner from '~shared/Spinner'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/LovtekstMedLenke'
import styled from 'styled-components'
import { BodyShort, ErrorMessage, Heading, Tabs } from '@navikt/ds-react'
import { TrygdeAvtale } from './avtaler/TrygdeAvtale'
import { IBehandlingStatus, IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { behandlingErIverksattEllerSamordnet } from '~components/behandling/felles/utils'
import { VedtakResultat } from '~components/behandling/useVedtaksResultat'
import { EnkelPersonTrygdetid } from '~components/behandling/trygdetid/EnkelPersonTrygdetid'
import { BeregnetSamletTrygdetid } from '~components/behandling/trygdetid/detaljer/BeregnetSamletTrygdetid'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

const TrygdetidMelding = ({ overskrift, beskrivelse }: { overskrift: string; beskrivelse: string }) => {
  return (
    <TrygdetidWrapper>
      <Heading size="small" level="3">
        {overskrift}
      </Heading>
      <BodyShort>{beskrivelse}</BodyShort>
    </TrygdetidWrapper>
  )
}

interface Props {
  redigerbar: boolean
  behandling: IDetaljertBehandling
  vedtaksresultat: VedtakResultat | null
  virkningstidspunktEtterNyRegelDato: Boolean
}

const visTrydeavtale = (behandling: IDetaljertBehandling): Boolean => {
  return (
    behandling.boddEllerArbeidetUtlandet?.vurdereAvoededsTrygdeavtale ||
    (behandling.behandlingType === IBehandlingsType.REVURDERING &&
      behandling.revurderingsaarsak === Revurderingaarsak.SLUTTBEHANDLING_UTLAND)
  )
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

  const visFlereTrygdetider = useFeatureEnabledMedDefault('foreldreloes', false)

  const personopplysninger = usePersonopplysninger()

  const mapNavn = (fnr: string): string => {
    const opplysning = personopplysninger?.avdoede?.find(
      (personOpplysning) => personOpplysning.opplysning.foedselsnummer === fnr
    )?.opplysning

    if (!opplysning) {
      return fnr
    }

    return `${opplysning.fornavn} ${opplysning.etternavn} (${fnr})`
  }

  const oppdaterTrygdetider = (trygdetid: ITrygdetid[]) => {
    setTrygdetider(trygdetid)
    dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
  }

  const fetchTrygdetider = (behandlingId: string) => {
    fetchTrygdetid(behandlingId, (trygdetider: ITrygdetid[]) => {
      if (trygdetider === null || trygdetider.length == 0) {
        if (behandlingErIverksattEllerSamordnet(behandling.status)) {
          setHarPilotTrygdetid(true)
        } else if (redigerbar) {
          requestOpprettTrygdetid(behandling.id, (trygdetider: ITrygdetid[]) => {
            oppdaterTrygdetider(trygdetider)
          })
        } else if (vedtaksresultat === 'avslag') {
          setTrygdetidManglerVedAvslag(true)
        } else {
          setTrygdetidIdMangler(true)
          throw new Error('Kan ikke opprette trygdetid når readonly')
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
    <TrygdetidWrapper>
      {visTrydeavtale(behandling) && <TrygdeAvtale redigerbar={redigerbar} />}
      <LovtekstMedLenke
        tittel="Avdødes trygdetid"
        hjemler={[
          {
            tittel: '§ 3-5 Trygdetid ved beregning av ytelser',
            lenke: 'https://lovdata.no/pro/lov/1997-02-28-19/§3-5',
          },
          {
            tittel: '§ 3-7 Beregning trygdetid',
            lenke: 'https://lovdata.no/pro/lov/1997-02-28-19/§3-7',
          },
          {
            tittel: 'EØS-forordning 883/2004 artikkel 52',
            lenke: 'https://lovdata.no/pro/eu/32004r0883/ARTIKKEL_52',
          },
        ]}
        status={null}
      >
        <BodyShort>
          Faktisk trygdetid kan gis fra avdøde fylte 16 år til dødsfall. Hadde avdøde opptjent pensjonspoeng fra fylte
          67 år til og med 75 år, gis det også et helt års trygdetid for aktuelle poengår. Fremtidig trygdetid kan gis
          fra dødsfallet til og med kalenderåret avdøde hadde blitt 66 år. Trygdetiden beregnes med maks 40 år. Avdødes
          utenlandske trygdetid fra avtaleland skal legges til for alternativ prorata-beregning av ytelsen. Ulike
          avtaler skal ikke beregnes sammen. Hvis avdøde har uføretrygd, skal som hovedregel trygdetid lagt til grunn i
          uføretrygden benyttes.
        </BodyShort>
      </LovtekstMedLenke>

      {landListe && (
        <>
          {(trygdetider.length == 1 || !visFlereTrygdetider) && (
            <EnkelPersonTrygdetid
              redigerbar={redigerbar}
              behandling={behandling}
              trygdetid={trygdetider[0]}
              landListe={landListe}
              virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato}
              fetchTrygdetider={fetchTrygdetider}
            />
          )}
          {trygdetider.length > 1 && visFlereTrygdetider && (
            <>
              <HeadingWrapper size="medium" level="2">
                Det finnes flere avdøde - husk å oppdatere begge to
              </HeadingWrapper>

              <Tabs defaultValue={trygdetider[0].ident}>
                <Tabs.List>
                  {trygdetider.map((trygdetid) => (
                    <Tabs.Tab key={trygdetid.ident} value={trygdetid.ident} label={mapNavn(trygdetid.ident)} />
                  ))}
                </Tabs.List>
                {trygdetider.map((trygdetid) => (
                  <Tabs.Panel value={trygdetid.ident} key={trygdetid.ident}>
                    <HeadingWrapper size="small" level="3">
                      Trygdetid for {mapNavn(trygdetid.ident)}
                    </HeadingWrapper>

                    <EnkelPersonTrygdetid
                      redigerbar={redigerbar}
                      behandling={behandling}
                      trygdetid={trygdetid}
                      landListe={landListe}
                      virkningstidspunktEtterNyRegelDato={virkningstidspunktEtterNyRegelDato}
                      fetchTrygdetider={fetchTrygdetider}
                    />
                  </Tabs.Panel>
                ))}
              </Tabs>

              <OppsummeringWrapper>
                <HeadingWrapper size="medium" level="2">
                  Oppsummering av trygdetid for flere avdøde
                </HeadingWrapper>

                {trygdetider.map((trygdetid) => (
                  <div key={trygdetid.ident}>
                    {trygdetid.beregnetTrygdetid?.resultat ? (
                      <>
                        <HeadingWrapper size="small" level="3">
                          Trygdetid for {trygdetid.ident}
                        </HeadingWrapper>
                        <BeregnetSamletTrygdetid beregnetTrygdetid={trygdetid.beregnetTrygdetid.resultat} />
                      </>
                    ) : (
                      <BodyShort>Trygdetid for {trygdetid.ident} mangler</BodyShort>
                    )}
                  </div>
                ))}
              </OppsummeringWrapper>
            </>
          )}
        </>
      )}

      {(isPending(hentTrygdetidRequest) || isPending(hentAlleLandRequest)) && (
        <Spinner visible={true} label="Henter trygdetid" />
      )}
      {isPending(opprettTrygdetidRequest) && <Spinner visible={true} label="Oppretter trygdetid" />}

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

      {behandlingsIdMangler && <ErrorMessage>Finner ikke behandling - ID mangler</ErrorMessage>}
      {trygdetidIdMangler && <ErrorMessage>Finner ikke trygdetid - ID mangler</ErrorMessage>}
    </TrygdetidWrapper>
  )
}

const TrygdetidWrapper = styled.div`
  padding: 0 4em;
  max-width: 69em;
`

const HeadingWrapper = styled(Heading)`
  margin-top: 2em;
  margin-bottom: 1em;
`

const OppsummeringWrapper = styled.div`
  margin-bottom: 2em;
`
