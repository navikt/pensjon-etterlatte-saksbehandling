import { Box, Button } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { behandlingErRedigerbar } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import {
  hentBeregningsGrunnlagOMS,
  lagreBeregningsGrunnlagOMS,
  opprettEllerEndreBeregning,
} from '~shared/api/beregning'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregingsGrunnlagOMS,
  oppdaterBeregning,
  resetBeregning,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import React, { useEffect, useState } from 'react'
import {
  Beregning,
  BeregningsMetode,
  BeregningsMetodeBeregningsgrunnlag,
  InstitusjonsoppholdGrunnlagData,
  ReduksjonOMS,
} from '~shared/types/Beregning'
import { mapListeTilDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import Spinner from '~shared/Spinner'
import { handlinger } from '~components/behandling/handlinger/typer'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import InstitusjonsoppholdBeregning from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdBeregning'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/LovtekstMedLenke'
import { TrygdetidMetodeBruktOMS } from '~components/behandling/beregningsgrunnlag/felles/TrygdetidMetodeBruktOMS'
import { ApiErrorAlert } from '~ErrorBoundary'

const BeregningsgrunnlagOmstillingsstoenad = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const [beregningsgrunnlagOMSResult, beregningsgrunnlagOMSRequest] = useApiCall(hentBeregningsGrunnlagOMS)
  const [lagreBeregningsgrunnlagOMS, postBeregningsgrunnlag] = useApiCall(lagreBeregningsGrunnlagOMS)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const [institusjonsoppholdsGrunnlagData, setInstitusjonsoppholdsGrunnlagData] =
    useState<InstitusjonsoppholdGrunnlagData | null>(null)
  const [beregningsMetodeBeregningsgrunnlag, setBeregningsMetodeBeregningsgrunnlag] =
    useState<BeregningsMetodeBeregningsgrunnlag | null>(null)

  useEffect(() => {
    beregningsgrunnlagOMSRequest(behandling.id, (result) => {
      if (result) {
        dispatch(
          oppdaterBeregingsGrunnlagOMS({
            ...result,
            institusjonsopphold: result.institusjonsoppholdBeregningsgrunnlag,
          })
        )
        setBeregningsMetodeBeregningsgrunnlag(result.beregningsMetode)
      }
    })
  }, [])

  const onSubmit = () => {
    dispatch(resetBeregning())
    const beregningsgrunnlagOMS = {
      institusjonsopphold: institusjonsoppholdsGrunnlagData
        ? mapListeTilDto(institusjonsoppholdsGrunnlagData)
        : behandling.beregningsGrunnlag?.institusjonsopphold ?? [],
      beregningsMetode: beregningsMetodeBeregningsgrunnlag
        ? beregningsMetodeBeregningsgrunnlag
        : behandling.beregningsGrunnlag?.beregningsMetode ?? {
            beregningsMetode: BeregningsMetode.NASJONAL,
          },
    }

    postBeregningsgrunnlag(
      {
        behandlingId: behandling.id,
        grunnlag: beregningsgrunnlagOMS,
      },
      () =>
        postOpprettEllerEndreBeregning(behandling.id, (beregning: Beregning) => {
          dispatch(oppdaterBeregingsGrunnlagOMS(beregningsgrunnlagOMS))
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
          dispatch(oppdaterBeregning(beregning))
          next()
        })
    )
  }

  return (
    <>
      <>
        {mapResult(beregningsgrunnlagOMSResult, {
          pending: <Spinner visible label="Henter beregningsgrunnlag..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente beregningsgrunnlag'}</ApiErrorAlert>,
          success: (beregningsgrunnlag) => (
            <>
              <TrygdetidMetodeBruktOMS
                redigerbar={redigerbar}
                behandling={behandling}
                beregningsgrunnlag={beregningsgrunnlag}
              />
              <InstitusjonsoppholdBeregning
                reduksjonsTyper={ReduksjonOMS}
                behandling={behandling}
                onSubmit={(institusjonsoppholdGrunnlag) =>
                  setInstitusjonsoppholdsGrunnlagData(institusjonsoppholdGrunnlag)
                }
                institusjonsopphold={behandling.beregningsGrunnlagOMS?.institusjonsopphold}
                lovtekstMedLenke={
                  <LovtekstMedLenke
                    tittel="Institusjonsopphold"
                    hjemler={[
                      {
                        tittel: '§ 17-13.Ytelser til gjenlevende ektefelle under opphold i institusjon',
                        lenke: 'https://lovdata.no/lov/1997-02-28-19/§17-13',
                      },
                    ]}
                    status={null}
                  >
                    <p>
                      Omstillingsstønad kan reduseres som følge av opphold i en institusjon med fri kost og losji under
                      statlig ansvar eller tilsvarende institusjon i utlandet. Regelen gjelder ikke ved opphold i
                      somatiske sykehusavdelinger. Oppholdet må vare i tre måneder i tillegg til innleggelsesmåneden for
                      at stønaden skal bli redusert. Dersom vedkommende har faste og nødvendige utgifter til bolig, skal
                      stønaden ikke reduseres eller reduseres mindre enn hovedregelen sier. Ytelsen skal ikke reduseres
                      når etterlatte forsørger barn.
                    </p>
                  </LovtekstMedLenke>
                }
              />
            </>
          ),
        })}
      </>
      {isFailureHandler({ apiResult: endreBeregning, errorMessage: 'Kunne ikke opprette ny beregning' })}
      {isFailureHandler({ apiResult: lagreBeregningsgrunnlagOMS, errorMessage: 'lagreBeregningsgrunnlagOMS' })}

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Button
              variant="primary"
              onClick={onSubmit}
              loading={isPending(lagreBeregningsgrunnlagOMS) || isPending(endreBeregning)}
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

export default BeregningsgrunnlagOmstillingsstoenad
