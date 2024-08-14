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
  oppdaterBeregningsGrunnlagOMS,
  oppdaterBeregning,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import React, { useEffect, useState } from 'react'
import { Beregning, BeregningsGrunnlagOMSDto, BeregningsMetodeBeregningsgrunnlag } from '~shared/types/Beregning'
import Spinner from '~shared/Spinner'
import { handlinger } from '~components/behandling/handlinger/typer'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { BeregningsMetodeBrukt } from '~components/behandling/beregningsgrunnlag/beregningsMetode/BeregningsMetodeBrukt'
import { ApiErrorAlert } from '~ErrorBoundary'
import { InstitusjonsoppholdBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlag'
import { InstitusjonsoppholdHendelser } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdHendelser'
import { SakType } from '~shared/types/sak'

const BeregningsgrunnlagOmstillingsstoenad = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [visManglendeBeregningsgrunnlag, setVisManglendeBeregningsgrunnlag] = useState(false)

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const [beregningsgrunnlagOMSResult, beregningsgrunnlagOMSRequest] = useApiCall(hentBeregningsGrunnlagOMS)
  const [lagreBeregningsGrunnlagOMSResult, lagreBeregningsGrunnlagOMSRequest] = useApiCall(lagreBeregningsGrunnlagOMS)
  const [opprettEllerEndreBeregningResult, opprettEllerEndreBeregningRequest] = useApiCall(opprettEllerEndreBeregning)

  const onSubmit = () => {
    if (!behandling.beregningsGrunnlagOMS?.beregningsMetode) {
      setVisManglendeBeregningsgrunnlag(true)
      return
    }
    setVisManglendeBeregningsgrunnlag(false)

    opprettEllerEndreBeregningRequest(behandling.id, (beregning: Beregning) => {
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
      dispatch(oppdaterBeregning(beregning))
      next()
    })
  }

  const oppdaterBeregningsMetode = (
    beregningsMetode: BeregningsMetodeBeregningsgrunnlag,
    beregningsgrunnlag: BeregningsGrunnlagOMSDto | null
  ) => {
    const grunnlag = {
      ...beregningsgrunnlag,
      beregningsMetode,
      institusjonsopphold: behandling.beregningsGrunnlag?.institusjonsopphold,
    }
    lagreBeregningsGrunnlagOMSRequest(
      {
        behandlingId: behandling.id,
        grunnlag,
      },
      () => {
        dispatch(oppdaterBeregningsGrunnlagOMS(grunnlag))
        setVisManglendeBeregningsgrunnlag(false)
      }
    )
  }

  useEffect(() => {
    beregningsgrunnlagOMSRequest(behandling.id, (result) => {
      if (result) {
        dispatch(
          oppdaterBeregningsGrunnlagOMS({
            ...result,
            institusjonsopphold: result.institusjonsoppholdBeregningsgrunnlag,
          })
        )
      }
    })
  }, [])

  return (
    <>
      <>
        {mapResult(beregningsgrunnlagOMSResult, {
          pending: <Spinner visible label="Henter beregningsgrunnlag..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente beregningsgrunnlag'}</ApiErrorAlert>,
          success: (beregningsgrunnlag) => (
            <>
              <BeregningsMetodeBrukt
                redigerbar={redigerbar}
                oppdaterBeregningsMetode={(beregningsMetode) =>
                  oppdaterBeregningsMetode(beregningsMetode, beregningsgrunnlag)
                }
                eksisterendeMetode={beregningsgrunnlag?.beregningsMetode}
                lagreBeregrningsGrunnlagResult={lagreBeregningsGrunnlagOMSResult}
              />

              <Box maxWidth="70rem">
                <InstitusjonsoppholdHendelser sakId={behandling.sakId} sakType={behandling.sakType} />
              </Box>

              <InstitusjonsoppholdBeregningsgrunnlag
                redigerbar={redigerbar}
                behandling={behandling}
                sakType={SakType.OMSTILLINGSSTOENAD}
                beregningsgrunnlag={behandling.beregningsGrunnlagOMS}
                institusjonsopphold={behandling.beregningsGrunnlagOMS?.institusjonsopphold}
              />
            </>
          ),
        })}
      </>
      {isFailureHandler({
        apiResult: opprettEllerEndreBeregningResult,
        errorMessage: 'Kunne ikke opprette eller oppdatere beregning',
      })}
      {visManglendeBeregningsgrunnlag && (
        <ApiErrorAlert>Beregningsmetode for trygdetid må velges før ytelsen kan beregnes.</ApiErrorAlert>
      )}

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Button
              variant="primary"
              onClick={onSubmit}
              loading={isPending(lagreBeregningsGrunnlagOMSResult) || isPending(opprettEllerEndreBeregningResult)}
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
