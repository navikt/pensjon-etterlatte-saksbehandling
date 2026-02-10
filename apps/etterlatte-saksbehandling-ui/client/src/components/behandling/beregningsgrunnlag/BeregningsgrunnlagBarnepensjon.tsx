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
  resetBeregning,
} from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import {
  mapListeTilDto,
  periodisertBeregningsgrunnlagTilDto,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import React, { useContext, useEffect, useState } from 'react'
import Soeskenjustering, {
  Soeskengrunnlag,
} from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import Spinner from '~shared/Spinner'
import { hentLevendeSoeskenFraAvdoedeForSoeker, IPdlPerson } from '~shared/types/Person'
import {
  Beregning,
  BeregningsMetodeBeregningsgrunnlagForm,
  tilBeregningsMetodeBeregningsgrunnlag,
  LagreBeregningsGrunnlagDto,
  toLagreBeregningsGrunnlagDto,
} from '~shared/types/Beregning'
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
import { BeregningsmetoderFlereAvdoede } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsmetoderFlereAvdoede'
import { useBehandling } from '~components/behandling/useBehandling'
import { mapNavn } from '~components/behandling/beregningsgrunnlag/Beregningsgrunnlag'
import { AnnenForelderVurdering } from '~shared/types/grunnlag'

const BeregningsgrunnlagBarnepensjon = () => {
  const { next } = useContext(BehandlingRouteContext)
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
        dispatch(oppdaterBeregningsGrunnlag(result))
      }
      hentTrygdetiderRequest(behandling.id)
    })
  }, [])

  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const kunEnJuridiskForelderPersongalleri =
    personopplysninger?.annenForelder?.vurdering === AnnenForelderVurdering.KUN_EN_REGISTRERT_JURIDISK_FORELDER

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

  const oppdaterBeregningsgrunnlag = (beregningsMetodeForm: BeregningsMetodeBeregningsgrunnlagForm) => {
    const grunnlag: LagreBeregningsGrunnlagDto = {
      ...toLagreBeregningsGrunnlagDto(behandling.beregningsGrunnlag),
      beregningsMetodeFlereAvdoede: undefined,
      beregningsMetode: tilBeregningsMetodeBeregningsgrunnlag(beregningsMetodeForm),
      kunEnJuridiskForelder: kunEnJuridiskForelderPersongalleri
        ? periodisertBeregningsgrunnlagTilDto({
            data: {},
            fom: new Date(behandling.virkningstidspunkt!!.dato),
            tom: beregningsMetodeForm.datoTilKunEnJuridiskForelder,
          })
        : undefined,
    }
    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag: grunnlag,
      },
      (result) => {
        dispatch(oppdaterBeregningsGrunnlag(result))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
      }
    )
  }

  const oppdaterSoeskenJustering = (soeskenGrunnlag: Soeskengrunnlag) => {
    const grunnlag: LagreBeregningsGrunnlagDto = {
      ...toLagreBeregningsGrunnlagDto(behandling.beregningsGrunnlag),
      soeskenMedIBeregning: mapListeTilDto(soeskenGrunnlag),
    }

    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag: grunnlag,
      },
      (result) => {
        dispatch(oppdaterBeregningsGrunnlag(result))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
      }
    )
  }

  const tidligsteAvdoede: IPdlPerson | null = personopplysninger?.avdoede?.length
    ? personopplysninger?.avdoede
        .map((it) => it.opplysning)
        .reduce((previous, current) => {
          return current.doedsdato!! < previous.doedsdato!! ? current : previous
        })
    : null

  return (
    <>
      <>
        {mapResult(hentBeregningsgrunnlagResult, {
          pending: <Spinner label="Henter beregningsgrunnlag..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente beregningsgrunnlag'}</ApiErrorAlert>,
          success: () =>
            mapResult(hentTrygdetiderResult, {
              pending: <Spinner label="Henter trygdetider..." />,
              error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente trygdetider'}</ApiErrorAlert>,
              success: (trygdetider) => (
                <>
                  {trygdetider && (
                    <>
                      {trygdetider?.length &&
                        trygdetider.length > 1 &&
                        (!!tidligsteAvdoede ? (
                          <BeregningsmetoderFlereAvdoede
                            redigerbar={redigerbar}
                            trygdetider={trygdetider}
                            tidligsteAvdoede={tidligsteAvdoede}
                          />
                        ) : (
                          <ApiErrorAlert>
                            Fant ikke avdøde i persongalleriet. For å beregne barnepensjonen riktig må det være en eller
                            flere avdøde i persongalleriet.
                          </ApiErrorAlert>
                        ))}
                      {trygdetider.length === 1 && (
                        <BeregningsMetodeBrukt
                          redigerbar={redigerbar}
                          navn={mapNavn(trygdetider[0].ident, personopplysninger)}
                          behandling={behandling}
                          oppdaterBeregningsgrunnlag={oppdaterBeregningsgrunnlag}
                          lagreBeregningsGrunnlagResult={lagreBeregningsgrunnlagResult}
                        />
                      )}
                    </>
                  )}

                  <Box maxWidth="70rem">
                    <InstitusjonsoppholdHendelser sakId={behandling.sakId} />
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

      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0">
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
