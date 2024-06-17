import { Box, Button, Heading, Tabs } from '@navikt/ds-react'
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
import InstitusjonsoppholdBeregning from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdBeregning'
import Soeskenjustering, {
  Soeskengrunnlag,
} from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import Spinner from '~shared/Spinner'
import { formaterNavn, hentLevendeSoeskenFraAvdoedeForSoeker } from '~shared/types/Person'
import {
  Beregning,
  BeregningsMetode,
  BeregningsMetodeBeregningsgrunnlag,
  BeregningsmetodeFlereAvdoedeData,
  BeregningsmetodeForAvdoed,
  InstitusjonsoppholdGrunnlagData,
} from '~shared/types/Beregning'
import BeregningsgrunnlagMetode from './BeregningsgrunnlagMetode'
import { handlinger } from '~components/behandling/handlinger/typer'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { isPending, isPendingOrInitial, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { behandlingGjelderBarnepensjonPaaNyttRegelverk } from '~components/behandling/vilkaarsvurdering/utils'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { hentTrygdetider, ITrygdetid } from '~shared/api/trygdetid'
import BeregningsgrunnlagMetodeForAvdoed from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagMetodeForAvdoed'
import BeregningsgrunnlagBarnepensjonOppsummering from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagBarnepensjonOppsummering'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/LovtekstMedLenke'
import { BP_INSTITUSJONSOPPHOLD_HJEMLER } from '~components/behandling/virkningstidspunkt/utils'

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
  const [lagreBeregningsgrunnlag, postBeregningsgrunnlag] = useApiCall(lagreBeregningsGrunnlag)
  const [beregningsgrunnlag, fetchBeregningsgrunnlag] = useApiCall(hentBeregningsGrunnlag)
  const [trygdetider, fetchTrygdetider] = useApiCall(hentTrygdetider)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const [soeskenGrunnlagsData, setSoeskenGrunnlagsData] = useState<Soeskengrunnlag | null>(null)
  const [institusjonsoppholdsGrunnlagData, setInstitusjonsoppholdsGrunnlagData] =
    useState<InstitusjonsoppholdGrunnlagData | null>(null)
  const [beregningsMetodeBeregningsgrunnlag, setBeregningsMetodeBeregningsgrunnlag] =
    useState<BeregningsMetodeBeregningsgrunnlag | null>(null)
  const [beregningsmetodeForAvdoede, setBeregningmetodeForAvdoede] = useState<BeregningsmetodeFlereAvdoedeData | null>(
    null
  )
  const [trygdetidsListe, setTrygdetidsListe] = useState<ITrygdetid[]>([])

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

  useEffect(() => {
    fetchBeregningsgrunnlag(behandling.id, (result) => {
      if (result) {
        dispatch(
          oppdaterBeregingsGrunnlag({ ...result, institusjonsopphold: result.institusjonsoppholdBeregningsgrunnlag })
        )
        setBeregningsMetodeBeregningsgrunnlag(result.beregningsMetode)
        if (result.begegningsmetodeFlereAvdoede) {
          setBeregningmetodeForAvdoede(mapListeFraDto(result.begegningsmetodeFlereAvdoede))
        }
      }
    })

    fetchTrygdetider(behandling.id, (result) => {
      if (result && result.length > 1) {
        setTrygdetidsListe(result)
      }
    })
  }, [])

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
        beregningsMetode: beregningsMetodeBeregningsgrunnlag
          ? beregningsMetodeBeregningsgrunnlag
          : behandling.beregningsGrunnlag?.beregningsMetode ?? {
              beregningsMetode: BeregningsMetode.NASJONAL,
            },
        begegningsmetodeFlereAvdoede: beregningsmetodeForAvdoede
          ? mapListeTilDto(beregningsmetodeForAvdoede)
          : behandling.beregningsGrunnlag?.begegningsmetodeFlereAvdoede ?? [],
      }

      postBeregningsgrunnlag(
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

  return (
    <>
      <>
        {isSuccess(beregningsgrunnlag) && isSuccess(trygdetider) && (
          <>
            {trygdetider.data.length > 1 && (
              <Box paddingBlock="16" paddingInline="16">
                {redigerbar && (
                  <>
                    <Heading size="medium" level="2">
                      Det finnes flere avdøde - husk å oppdatere begge to
                    </Heading>

                    <Tabs defaultValue={trygdetider.data[0].ident}>
                      <Tabs.List>
                        {trygdetider.data.map((trygdetid) => (
                          <Tabs.Tab key={trygdetid.ident} value={trygdetid.ident} label={mapNavn(trygdetid.ident)} />
                        ))}
                      </Tabs.List>
                      {trygdetider.data.map((trygdetid) => (
                        <Tabs.Panel value={trygdetid.ident} key={trygdetid.ident}>
                          <BeregningsgrunnlagMetodeForAvdoed
                            ident={trygdetid.ident}
                            navn={mapNavn(trygdetid.ident)}
                            grunnlag={periodisertBeregningsmetodeForAvdoed(trygdetid.ident)}
                            onUpdate={(data: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>) => {
                              oppdaterPeriodisertBeregningsmetodeForAvdoed(data)
                            }}
                          />
                        </Tabs.Panel>
                      ))}
                    </Tabs>
                  </>
                )}

                <BeregningsgrunnlagBarnepensjonOppsummering
                  trygdetider={trygdetider.data}
                  mapNavn={mapNavn}
                  periodisertBeregningsmetodeForAvdoed={periodisertBeregningsmetodeForAvdoed}
                />
              </Box>
            )}

            {trygdetidsListe.length <= 1 && (
              <BeregningsgrunnlagMetode
                redigerbar={redigerbar}
                grunnlag={beregningsMetodeBeregningsgrunnlag}
                onUpdate={(grunnlag) => {
                  setBeregningsMetodeBeregningsgrunnlag({ ...grunnlag })
                }}
              />
            )}
          </>
        )}
        {isSuccess(beregningsgrunnlag) && skalViseSoeskenjustering && (
          <Soeskenjustering
            behandling={behandling}
            onSubmit={(soeskenGrunnlag) => setSoeskenGrunnlagsData(soeskenGrunnlag)}
            setSoeskenJusteringManglerIkke={() => setSoeskenJusteringMangler(false)}
          />
        )}
        {isSuccess(beregningsgrunnlag) && (
          <InstitusjonsoppholdBeregning
            behandling={behandling}
            onSubmit={(institusjonsoppholdGrunnlag) => setInstitusjonsoppholdsGrunnlagData(institusjonsoppholdGrunnlag)}
            institusjonsopphold={behandling.beregningsGrunnlag?.institusjonsopphold}
            lovtekstMedLenke={
              <LovtekstMedLenke tittel="Institusjonsopphold" hjemler={BP_INSTITUSJONSOPPHOLD_HJEMLER} status={null}>
                <p>
                  Barnepensjonen skal reduseres under opphold i en institusjon med fri kost og losji under statlig
                  ansvar eller tilsvarende institusjon i utlandet. Regelen gjelder ikke ved opphold i somatiske
                  sykehusavdelinger. Oppholdet må vare i tre måneder i tillegg til innleggelsesmåneden for at
                  barnepensjonen skal bli redusert. Dersom barnet har faste og nødvendige utgifter til bolig, kan
                  arbeids- og velferdsetaten bestemme at barnepensjonen ikke skal reduseres eller reduseres mindre enn
                  hovedregelen sier.
                </p>
              </LovtekstMedLenke>
            }
          />
        )}
        <Spinner visible={isPending(beregningsgrunnlag)} label="Henter beregningsgrunnlag" />
        {isFailureHandler({
          apiResult: beregningsgrunnlag,
          errorMessage: 'Beregningsgrunnlag kan ikke hentes',
        })}
      </>
      {manglerSoeskenJustering && <ApiErrorAlert>Søskenjustering er ikke fylt ut </ApiErrorAlert>}
      {isFailureHandler({
        apiResult: endreBeregning,
        errorMessage: 'Kunne ikke opprette ny beregning',
      })}
      {isFailureHandler({
        apiResult: lagreBeregningsgrunnlag,
        errorMessage: 'Kunne ikke lagre beregningsgrunnlag',
      })}
      {isFailureHandler({
        apiResult: trygdetider,
        errorMessage: 'Kunne ikke hente trygdetid(er)',
      })}
      {isPendingOrInitial(trygdetider) && <Spinner visible={true} label="Henter trygdetidsoversikt ..." />}

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Button
              variant="primary"
              onClick={onSubmit}
              loading={isPending(lagreBeregningsgrunnlag) || isPending(endreBeregning)}
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
