/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

import { Box, Button } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { BehandlingRouteContext } from '../BehandlingRoutes'
import { behandlingErRedigerbar } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import { hentBeregningsGrunnlag, lagreBeregningsGrunnlag, opprettEllerEndreBeregning } from '~shared/api/beregning'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  oppdaterBehandlingsstatus,
  oppdaterBeregning,
  oppdaterBeregningsGrunnlag,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import React, { useContext, useEffect, useState } from 'react'
import {
  Beregning,
  BeregningsMetodeBeregningsgrunnlagForm,
  tilBeregningsMetodeBeregningsgrunnlag,
  LagreBeregningsGrunnlagDto,
  toLagreBeregningsGrunnlagDto,
} from '~shared/types/Beregning'
import Spinner from '~shared/Spinner'
import { handlinger } from '~components/behandling/handlinger/typer'
import { isPending, mapFailure, mapResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { BeregningsMetodeBrukt } from '~components/behandling/beregningsgrunnlag/beregningsMetode/BeregningsMetodeBrukt'
import { ApiErrorAlert } from '~ErrorBoundary'
import { InstitusjonsoppholdBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlag'
import { InstitusjonsoppholdHendelser } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdHendelser'
import { SakType } from '~shared/types/sak'
import { useBehandling } from '~components/behandling/useBehandling'
import { mapNavn } from '~components/behandling/beregningsgrunnlag/Beregningsgrunnlag'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { formaterNavn } from '~shared/types/Person'

const BeregningsgrunnlagOmstillingsstoenad = () => {
  const behandling = useBehandling()
  const personopplysninger = usePersonopplysninger()
  const { next } = useContext(BehandlingRouteContext)
  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [visManglendeBeregningsgrunnlag, setVisManglendeBeregningsgrunnlag] = useState(false)

  const [hentBeregningsgrunnlagResult, hentBeregningsgrunnlagRequest] = useApiCall(hentBeregningsGrunnlag)
  const [lagreBeregningsGrunnlagResult, lagreBeregningsGrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)
  const [opprettEllerEndreBeregningResult, opprettEllerEndreBeregningRequest] = useApiCall(opprettEllerEndreBeregning)

  if (!behandling) return <ApiErrorAlert>Fant ikke behandling</ApiErrorAlert>

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const onSubmit = () => {
    if (!behandling.beregningsGrunnlag?.beregningsMetode) {
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

  const oppdaterBeregningsMetode = (beregningsMetodeForm: BeregningsMetodeBeregningsgrunnlagForm) => {
    const grunnlag: LagreBeregningsGrunnlagDto = {
      ...toLagreBeregningsGrunnlagDto(behandling?.beregningsGrunnlag),
      beregningsMetodeFlereAvdoede: undefined,
      beregningsMetode: tilBeregningsMetodeBeregningsgrunnlag(beregningsMetodeForm),
    }
    lagreBeregningsGrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag: grunnlag,
      },
      (result) => {
        dispatch(oppdaterBeregningsGrunnlag(result))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
        setVisManglendeBeregningsgrunnlag(false)
      }
    )
  }

  useEffect(() => {
    hentBeregningsgrunnlagRequest(behandling.id, (result) => {
      if (result) {
        dispatch(oppdaterBeregningsGrunnlag(result))
      }
    })
  }, [])

  const tidligereFamiliepleier = !!behandling.tidligereFamiliepleier?.svar

  const hentNavn = () => {
    if (personopplysninger?.avdoede[0]) {
      return mapNavn(personopplysninger.avdoede[0].opplysning.foedselsnummer, personopplysninger)
    } else if (tidligereFamiliepleier && personopplysninger?.soeker) {
      return formaterNavn(personopplysninger.soeker.opplysning)
    }
    return ''
  }

  return (
    <>
      <>
        {mapResult(hentBeregningsgrunnlagResult, {
          pending: <Spinner label="Henter beregningsgrunnlag..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente beregningsgrunnlag'}</ApiErrorAlert>,
          success: () => (
            <>
              <BeregningsMetodeBrukt
                redigerbar={redigerbar}
                navn={hentNavn()}
                behandling={behandling}
                oppdaterBeregningsgrunnlag={oppdaterBeregningsMetode}
                lagreBeregningsGrunnlagResult={lagreBeregningsGrunnlagResult}
              />

              {mapFailure(lagreBeregningsGrunnlagResult, (error) => (
                <ApiErrorAlert>{error.detail}</ApiErrorAlert>
              ))}

              <Box maxWidth="70rem">
                <InstitusjonsoppholdHendelser sakId={behandling.sakId} />
              </Box>
              <InstitusjonsoppholdBeregningsgrunnlag
                redigerbar={redigerbar}
                behandling={behandling}
                sakType={SakType.OMSTILLINGSSTOENAD}
                beregningsgrunnlag={behandling.beregningsGrunnlag}
                institusjonsopphold={behandling.beregningsGrunnlag?.institusjonsopphold}
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

      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Button
              variant="primary"
              onClick={onSubmit}
              loading={isPending(lagreBeregningsGrunnlagResult) || isPending(opprettEllerEndreBeregningResult)}
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
