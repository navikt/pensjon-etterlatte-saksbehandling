import { Box, Button } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { behandlingErRedigerbar } from '../felles/utils'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { useAppDispatch } from '~store/Store'
import { hentBeregningsGrunnlag, lagreBeregningsGrunnlag, opprettEllerEndreBeregning } from '~shared/api/beregning'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  oppdaterBehandlingsstatus,
  oppdaterBeregingsGrunnlag,
  oppdaterBeregning,
  resetBeregning,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import { mapListeTilDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import React, { useEffect, useState } from 'react'
import Soeskenjustering, {
  Soeskengrunnlag,
} from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import Spinner from '~shared/Spinner'
import { hentLevendeSoeskenFraAvdoedeForSoeker } from '~shared/types/Person'
import { Beregning, BeregningsMetode, BeregningsMetodeBeregningsgrunnlag } from '~shared/types/Beregning'
import { handlinger } from '~components/behandling/handlinger/typer'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { behandlingGjelderBarnepensjonPaaNyttRegelverk } from '~components/behandling/vilkaarsvurdering/utils'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { hentTrygdetider } from '~shared/api/trygdetid'
import { BeregningsMetodeBrukt } from '~components/behandling/beregningsgrunnlag/beregningsMetode/BeregningsMetodeBrukt'
import { InstitusjonsoppholdHendelser } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdHendelser'
import { InstitusjonsoppholdBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlag'
import { SakType } from '~shared/types/sak'
import { BeregningsgrunnlagFlereAvdoede } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsgrunnlagFlereAvdoede'
import { useBehandling } from '~components/behandling/useBehandling'

const BeregningsgrunnlagBarnepensjon = () => {
  const { next } = useBehandlingRoutes()
  const personopplysninger = usePersonopplysninger()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const behandling = useBehandling()
  const dispatch = useAppDispatch()
  const [lagreBeregningsgrunnlagResult, lagreBeregningsgrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)
  const [hentBeregningsgrunnlagResult, hentBeregningsgrunnlagRequest] = useApiCall(hentBeregningsGrunnlag)
  const [hentTrygdetiderResult, hentTrygdetiderRequest] = useApiCall(hentTrygdetider)
  const [opprettEllerEndreBeregningResult, opprettEllerEndreBeregningRequest] = useApiCall(opprettEllerEndreBeregning)

  const [manglerSoeskenJustering, setSoeskenJusteringMangler] = useState<boolean>(false)

  if (!behandling) return <ApiErrorAlert>Fant ikke behandling</ApiErrorAlert>

  useEffect(() => {
    hentBeregningsgrunnlagRequest(behandling.id, (result) => {
      if (result) {
        dispatch(
          oppdaterBeregingsGrunnlag({ ...result, institusjonsopphold: result.institusjonsoppholdBeregningsgrunnlag })
        )
      }
      hentTrygdetiderRequest(behandling.id)
    })
  }, [])

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  if (behandling.kommerBarnetTilgode == null) {
    return <ApiErrorAlert>Familieforhold kan ikke hentes ut</ApiErrorAlert>
  }

  const soesken =
    (personopplysninger &&
      hentLevendeSoeskenFraAvdoedeForSoeker(
        personopplysninger.avdoede,
        personopplysninger.soeker?.opplysning.foedselsnummer as string
      )) ??
    []
  const skalViseSoeskenjustering = soesken.length > 0 && !behandlingGjelderBarnepensjonPaaNyttRegelverk(behandling)

  const onSubmit = () => {
    // Todo: dis dont work, ListIsEmpty
    if (skalViseSoeskenjustering && !behandling.beregningsGrunnlag?.soeskenMedIBeregning) {
      setSoeskenJusteringMangler(true)
    } else {
      opprettEllerEndreBeregningRequest(behandling.id, (beregning: Beregning) => {
        dispatch(resetBeregning())
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
        dispatch(oppdaterBeregning(beregning))
        next()
      })
    }
  }

  const oppdaterBeregningsMetode = (beregningsMetode: BeregningsMetodeBeregningsgrunnlag) => {
    const grunnlag = {
      ...behandling.beregningsGrunnlag,
      beregningsMetode,
      institusjonsopphold: behandling.beregningsGrunnlag?.institusjonsopphold,
      begegningsmetodeFlereAvdoede: behandling.beregningsGrunnlag?.begegningsmetodeFlereAvdoede,
      soeskenMedIBeregning: behandling.beregningsGrunnlag?.soeskenMedIBeregning ?? [],
    }
    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag,
      },
      () => dispatch(oppdaterBeregingsGrunnlag(grunnlag))
    )
  }

  const oppdaterSoeskenJustering = (soeskenGrunnlag: Soeskengrunnlag) => {
    const grunnlag = {
      ...behandling.beregningsGrunnlag,
      soeskenMedIBeregning: mapListeTilDto(soeskenGrunnlag),
      institusjonsopphold: behandling.beregningsGrunnlag?.institusjonsopphold,
      begegningsmetodeFlereAvdoede: behandling.beregningsGrunnlag?.begegningsmetodeFlereAvdoede,
      beregningsMetode: behandling.beregningsGrunnlag?.beregningsMetode ?? {
        beregningsMetode: BeregningsMetode.NASJONAL,
      },
    }

    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag,
      },
      () => dispatch(oppdaterBeregingsGrunnlag(grunnlag))
    )
  }

  return (
    <>
      <>
        {mapResult(hentBeregningsgrunnlagResult, {
          pending: <Spinner visible label="Henter beregningsgrunnlag..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente beregningsgrunnlag'}</ApiErrorAlert>,
          success: () =>
            mapResult(hentTrygdetiderResult, {
              pending: <Spinner visible label="Henter trygdetider..." />,
              error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente trygdetider'}</ApiErrorAlert>,
              success: (trygdetider) => (
                <>
                  {trygdetider.length > 1 && (
                    <BeregningsgrunnlagFlereAvdoede redigerbar={redigerbar} trygdetider={trygdetider} />
                  )}
                  {trygdetider.length <= 1 && (
                    <BeregningsMetodeBrukt
                      redigerbar={redigerbar}
                      oppdaterBeregningsMetode={(beregningsMetode) => oppdaterBeregningsMetode(beregningsMetode)}
                      eksisterendeMetode={behandling?.beregningsGrunnlag?.beregningsMetode}
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
                  {skalViseSoeskenjustering && (
                    <Soeskenjustering
                      behandling={behandling}
                      onSubmit={(soeskenGrunnlag) => oppdaterSoeskenJustering(soeskenGrunnlag)}
                      setSoeskenJusteringManglerIkke={() => setSoeskenJusteringMangler(false)}
                    />
                  )}
                </>
              ),
            }),
        })}
      </>

      {manglerSoeskenJustering && <ApiErrorAlert>Søskenjustering er ikke fylt ut </ApiErrorAlert>}

      {isFailureHandler({
        apiResult: opprettEllerEndreBeregningResult,
        errorMessage: 'Kunne ikke opprette ny beregning',
      })}
      {isFailureHandler({
        apiResult: hentTrygdetiderResult,
        errorMessage: 'Kunne ikke hente trygdetid(er)',
      })}
      {isFailureHandler({
        apiResult: lagreBeregningsgrunnlagResult,
        errorMessage: 'Kunne ikke lagre beregningsgrunnlag',
      })}

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        {redigerbar ? (
          <BehandlingHandlingKnapper>
            <Button
              variant="primary"
              onClick={onSubmit}
              loading={isPending(lagreBeregningsgrunnlagResult) || isPending(opprettEllerEndreBeregningResult)}
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
