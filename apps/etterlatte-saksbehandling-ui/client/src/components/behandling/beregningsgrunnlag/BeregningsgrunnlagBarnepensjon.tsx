import { Box, Button } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { behandlingErRedigerbar } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import { hentBeregningsGrunnlag, lagreBeregningsGrunnlag, opprettEllerEndreBeregning } from '~shared/api/beregning'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregingsGrunnlag,
  oppdaterBeregning,
  resetBeregning,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import {
  mapListeFraDto,
  mapListeTilDto,
  PeriodisertBeregningsgrunnlag,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import React, { useEffect, useState } from 'react'
import { Soeskengrunnlag } from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import Spinner from '~shared/Spinner'
import { formaterNavn, hentLevendeSoeskenFraAvdoedeForSoeker } from '~shared/types/Person'
import {
  Beregning,
  BeregningsGrunnlagDto,
  BeregningsMetode,
  BeregningsMetodeBeregningsgrunnlag,
  BeregningsmetodeFlereAvdoedeData,
  BeregningsmetodeForAvdoed,
  InstitusjonsoppholdGrunnlagData,
} from '~shared/types/Beregning'
import { handlinger } from '~components/behandling/handlinger/typer'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { behandlingGjelderBarnepensjonPaaNyttRegelverk } from '~components/behandling/vilkaarsvurdering/utils'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { hentTrygdetider } from '~shared/api/trygdetid'
import { BeregningsMetodeBrukt } from '~components/behandling/beregningsgrunnlag/BeregningsMetodeBrukt'
import { InstitusjonsoppholdHendelser } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdHendelser'
import { InstitusjonsoppholdBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlag'
import { SakType } from '~shared/types/sak'

const BeregningsgrunnlagBarnepensjon = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const personopplysninger = usePersonopplysninger()
  const avdoede = personopplysninger?.avdoede.find((po) => po)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const dispatch = useAppDispatch()
  const [lagreBeregningsgrunnlagResult, lagreBeregningsgrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)
  const [beregningsgrunnlagResult, beregningsgrunnlagRequest] = useApiCall(hentBeregningsGrunnlag)
  const [trygdetiderResult, trygdetiderRequest] = useApiCall(hentTrygdetider)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const [soeskenGrunnlagsData, setSoeskenGrunnlagsData] = useState<Soeskengrunnlag | null>(null)
  const [institusjonsoppholdsGrunnlagData, setInstitusjonsoppholdsGrunnlagData] =
    useState<InstitusjonsoppholdGrunnlagData | null>(null)
  const [beregningsmetodeForAvdoede, setBeregningmetodeForAvdoede] = useState<BeregningsmetodeFlereAvdoedeData | null>(
    null
  )

  const [manglerSoeskenJustering, setSoeskenJusteringMangler] = useState<boolean>(false)

  const mapNavn = (fnr: string): string => {
    const opplysning = personopplysninger?.avdoede?.find(
      (personOpplysning) => personOpplysning.opplysning.foedselsnummer === fnr
    )?.opplysning

    if (!opplysning) {
      return fnr
    }

    return `${formaterNavn(opplysning)} (${fnr})`
  }

  if (behandling.kommerBarnetTilgode == null) {
    return <ApiErrorAlert>Familieforhold kan ikke hentes ut</ApiErrorAlert>
  }

  const soesken =
    (avdoede &&
      hentLevendeSoeskenFraAvdoedeForSoeker(
        avdoede,
        personopplysninger?.soeker?.opplysning.foedselsnummer as string
      )) ??
    []
  const skalViseSoeskenjustering = soesken.length > 0 && !behandlingGjelderBarnepensjonPaaNyttRegelverk(behandling)

  const periodisertBeregningsmetodeForAvdoed = (
    ident: String
  ): PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | null =>
    beregningsmetodeForAvdoede?.find((grunnlag) => grunnlag?.data.avdoed === ident) || null

  const oppdaterPeriodisertBeregningsmetodeForAvdoed = (
    grunnlag: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>
  ) => {
    const oppdaterState = beregningsmetodeForAvdoede
      ? beregningsmetodeForAvdoede
      : mapListeFraDto<BeregningsmetodeForAvdoed>([])

    setBeregningmetodeForAvdoede(
      oppdaterState.filter((data) => data.data.avdoed !== grunnlag.data.avdoed).concat(grunnlag)
    )
  }

  const onSubmit = () => {
    if (skalViseSoeskenjustering && !(soeskenGrunnlagsData || behandling.beregningsGrunnlag?.soeskenMedIBeregning)) {
      setSoeskenJusteringMangler(true)
    }
    if (behandling.beregningsGrunnlag?.soeskenMedIBeregning || soeskenGrunnlagsData || !skalViseSoeskenjustering) {
      dispatch(resetBeregning())
      const beregningsgrunnlag = {
        soeskenMedIBeregning: soeskenGrunnlagsData
          ? mapListeTilDto(soeskenGrunnlagsData)
          : behandling.beregningsGrunnlag?.soeskenMedIBeregning ?? [],
        institusjonsopphold: institusjonsoppholdsGrunnlagData
          ? mapListeTilDto(institusjonsoppholdsGrunnlagData)
          : behandling.beregningsGrunnlag?.institusjonsopphold ?? [],
        beregningsMetode: behandling.beregningsGrunnlag?.beregningsMetode ?? {
          beregningsMetode: BeregningsMetode.NASJONAL,
        },
        begegningsmetodeFlereAvdoede: beregningsmetodeForAvdoede
          ? mapListeTilDto(beregningsmetodeForAvdoede)
          : behandling.beregningsGrunnlag?.begegningsmetodeFlereAvdoede ?? [],
      }

      lagreBeregningsgrunnlagRequest(
        {
          behandlingId: behandling.id,
          grunnlag: beregningsgrunnlag,
        },
        () =>
          postOpprettEllerEndreBeregning(behandling.id, (beregning: Beregning) => {
            dispatch(oppdaterBeregingsGrunnlag(beregningsgrunnlag))
            dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
            dispatch(oppdaterBeregning(beregning))
            next()
          })
      )
    }
  }

  const oppdaterBeregningsMetode = (
    beregningsMetode: BeregningsMetodeBeregningsgrunnlag,
    beregningsgrunnlag: BeregningsGrunnlagDto | null
  ) => {
    lagreBeregningsgrunnlagRequest({
      behandlingId: behandling.id,
      grunnlag: {
        ...beregningsgrunnlag,
        beregningsMetode,
        institusjonsopphold: behandling.beregningsGrunnlag?.institusjonsopphold,
        begegningsmetodeFlereAvdoede: behandling.beregningsGrunnlag?.begegningsmetodeFlereAvdoede,
        soeskenMedIBeregning: behandling.beregningsGrunnlag?.soeskenMedIBeregning ?? [],
      },
    })
  }

  useEffect(() => {
    beregningsgrunnlagRequest(behandling.id, (result) => {
      if (result) {
        dispatch(
          oppdaterBeregingsGrunnlag({ ...result, institusjonsopphold: result.institusjonsoppholdBeregningsgrunnlag })
        )
        if (result.begegningsmetodeFlereAvdoede) {
          setBeregningmetodeForAvdoede(mapListeFraDto(result.begegningsmetodeFlereAvdoede))
        }
      }
      trygdetiderRequest(behandling.id)
    })
  }, [])

  return (
    <>
      <>
        {mapResult(beregningsgrunnlagResult, {
          pending: <Spinner visible label="Henter beregningsgrunnlag..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente beregningsgrunnlag'}</ApiErrorAlert>,
          success: (beregningsgrunnlag) =>
            mapResult(trygdetiderResult, {
              pending: <Spinner visible label="Henter trygdetider..." />,
              error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente trygdetider'}</ApiErrorAlert>,
              success: (trygdetider) => (
                <>
                  {trygdetider.length <= 1 && (
                    <BeregningsMetodeBrukt
                      redigerbar={redigerbar}
                      oppdaterBeregningsMetode={(beregningsMetode) =>
                        oppdaterBeregningsMetode(beregningsMetode, beregningsgrunnlag)
                      }
                      eksisterendeMetode={beregningsgrunnlag?.beregningsMetode}
                      lagreBeregrningsGrunnlagResult={lagreBeregningsgrunnlagResult}
                    />
                  )}

                  <Box maxWidth="70rem">
                    <InstitusjonsoppholdHendelser sakId={behandling.sakId} sakType={behandling.sakType} />
                  </Box>

                  <InstitusjonsoppholdBeregningsgrunnlag
                    redigerbar={redigerbar}
                    behandling={behandling}
                    sakType={SakType.BARNEPENSJON}
                    beregningsgrunnlag={behandling.beregningsGrunnlag}
                    institusjonsopphold={behandling.beregningsGrunnlag?.institusjonsopphold}
                  />
                </>
              ),
            }),
        })}
        {/*{isSuccess(beregningsgrunnlag) && isSuccess(trygdetider) && (*/}
        {/*  <>*/}
        {/*    {trygdetider.data.length > 1 && (*/}
        {/*      <Box paddingBlock="16" paddingInline="16">*/}
        {/*        {redigerbar && (*/}
        {/*          <>*/}
        {/*            <Heading size="medium" level="2">*/}
        {/*              Det finnes flere avdøde - husk å oppdatere begge to*/}
        {/*            </Heading>*/}

        {/*            <Tabs defaultValue={trygdetider.data[0].ident}>*/}
        {/*              <Tabs.List>*/}
        {/*                {trygdetider.data.map((trygdetid) => (*/}
        {/*                  <Tabs.Tab key={trygdetid.ident} value={trygdetid.ident} label={mapNavn(trygdetid.ident)} />*/}
        {/*                ))}*/}
        {/*              </Tabs.List>*/}
        {/*              {trygdetider.data.map((trygdetid) => (*/}
        {/*                <Tabs.Panel value={trygdetid.ident} key={trygdetid.ident}>*/}
        {/*                  <BeregningsgrunnlagMetodeForAvdoed*/}
        {/*                    ident={trygdetid.ident}*/}
        {/*                    navn={mapNavn(trygdetid.ident)}*/}
        {/*                    grunnlag={periodisertBeregningsmetodeForAvdoed(trygdetid.ident)}*/}
        {/*                    onUpdate={(data: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>) => {*/}
        {/*                      oppdaterPeriodisertBeregningsmetodeForAvdoed(data)*/}
        {/*                    }}*/}
        {/*                  />*/}
        {/*                </Tabs.Panel>*/}
        {/*              ))}*/}
        {/*            </Tabs>*/}
        {/*          </>*/}
        {/*        )}*/}

        {/*        <BeregningsgrunnlagBarnepensjonOppsummering*/}
        {/*          trygdetider={trygdetider.data}*/}
        {/*          mapNavn={mapNavn}*/}
        {/*          periodisertBeregningsmetodeForAvdoed={periodisertBeregningsmetodeForAvdoed}*/}
        {/*        />*/}
        {/*      </Box>*/}
        {/*    )}*/}
        {/*  </>*/}
        {/*)}*/}
        {/*{isSuccess(beregningsgrunnlag) && skalViseSoeskenjustering && (*/}
        {/*  <Soeskenjustering*/}
        {/*    behandling={behandling}*/}
        {/*    onSubmit={(soeskenGrunnlag) => setSoeskenGrunnlagsData(soeskenGrunnlag)}*/}
        {/*    setSoeskenJusteringManglerIkke={() => setSoeskenJusteringMangler(false)}*/}
        {/*  />*/}
        {/*)}*/}
        {/*{isSuccess(beregningsgrunnlag) && (*/}
        {/*  <InstitusjonsoppholdBeregning*/}
        {/*    behandling={behandling}*/}
        {/*    onSubmit={(institusjonsoppholdGrunnlag) => setInstitusjonsoppholdsGrunnlagData(institusjonsoppholdGrunnlag)}*/}
        {/*    institusjonsopphold={behandling.beregningsGrunnlag?.institusjonsopphold}*/}
        {/*    lovtekstMedLenke={*/}
        {/*      <LovtekstMedLenke tittel="Institusjonsopphold" hjemler={BP_INSTITUSJONSOPPHOLD_HJEMLER} status={null}>*/}
        {/*        <p>*/}
        {/*          Barnepensjonen skal reduseres under opphold i en institusjon med fri kost og losji under statlig*/}
        {/*          ansvar eller tilsvarende institusjon i utlandet. Regelen gjelder ikke ved opphold i somatiske*/}
        {/*          sykehusavdelinger. Oppholdet må vare i tre måneder i tillegg til innleggelsesmåneden for at*/}
        {/*          barnepensjonen skal bli redusert. Dersom barnet har faste og nødvendige utgifter til bolig, kan*/}
        {/*          arbeids- og velferdsetaten bestemme at barnepensjonen ikke skal reduseres eller reduseres mindre enn*/}
        {/*          hovedregelen sier.*/}
        {/*        </p>*/}
        {/*      </LovtekstMedLenke>*/}
        {/*    }*/}
        {/*    reduksjonsTyper={ReduksjonBP}*/}
        {/*  />*/}
        {/*)}*/}
        {/*<Spinner visible={isPending(beregningsgrunnlag)} label="Henter beregningsgrunnlag" />*/}
        {/*{isFailureHandler({*/}
        {/*  apiResult: beregningsgrunnlag,*/}
        {/*  errorMessage: 'Beregningsgrunnlag kan ikke hentes',*/}
        {/*})}*/}
      </>
      {manglerSoeskenJustering && <ApiErrorAlert>Søskenjustering er ikke fylt ut </ApiErrorAlert>}
      {isFailureHandler({
        apiResult: endreBeregning,
        errorMessage: 'Kunne ikke opprette ny beregning',
      })}
      {isFailureHandler({
        apiResult: lagreBeregningsgrunnlagResult,
        errorMessage: 'Kunne ikke lagre beregningsgrunnlag',
      })}
      {isFailureHandler({
        apiResult: trygdetiderResult,
        errorMessage: 'Kunne ikke hente trygdetid(er)',
      })}

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Button
              variant="primary"
              onClick={onSubmit}
              loading={isPending(lagreBeregningsgrunnlagResult) || isPending(endreBeregning)}
            >
              {handlinger.NESTE.navn}
            </Button>
          </BehandlingHandlingKnapper>
        ) : (
          <NesteOgTilbake />
        )}
      </Box>
    </>
  )
}

export default BeregningsgrunnlagBarnepensjon
