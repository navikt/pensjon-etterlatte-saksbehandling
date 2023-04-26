import { Content, ContentHeader } from '~shared/styled'
import { HeadingWrapper } from '../soeknadsoversikt/styled'
import { behandlingSkalSendeBrev, hentBehandlesFraStatus } from '../felles/utils'
import { formaterStringDato } from '~utils/formattering'
import { formaterVedtaksResultat, useVedtaksResultat } from '../useVedtaksResultat'
import { useAppDispatch } from '~store/Store'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { useEffect, useState } from 'react'
import { hentBeregning } from '~shared/api/beregning'
import { IBehandlingReducer, oppdaterBehandlingsstatus, oppdaterBeregning } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Alert, Button, ErrorMessage, Heading } from '@navikt/ds-react'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { upsertVedtak } from '~shared/api/behandling'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import styled from 'styled-components'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/sendTilAttesteringModal'
import { Beregningstype } from '~shared/types/Beregning'
import { BarnepensjonSammendrag } from '~components/behandling/beregne/BarnepensjonSammendrag'
import { OmstillingsstoenadSammendrag } from '~components/behandling/beregne/OmstillingsstoenadSammendrag'
import { Avkorting } from '~components/behandling/beregne/Avkorting'
import { ISaksType } from '~components/behandling/fargetags/saksType'

export const Beregne = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const { next } = useBehandlingRoutes()
  const beregningFraState = behandling.beregning
  const dispatch = useAppDispatch()
  const [beregning, hentBeregningRequest] = useApiCall(hentBeregning)
  const [vedtak, oppdaterVedtakRequest] = useApiCall(upsertVedtak)
  const [visAttesteringsmodal, setVisAttesteringsmodal] = useState(false)

  useEffect(() => {
    if (!beregningFraState) {
      hentBeregningRequest(behandling.id, (res) => dispatch(oppdaterBeregning(res)))
    }
  }, [])

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const vedtaksresultat =
    behandling.behandlingType !== IBehandlingsType.MANUELT_OPPHOER ? useVedtaksResultat() : 'opphoer'

  const opprettEllerOppdaterVedtak = () => {
    oppdaterVedtakRequest(behandling.id, () => {
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
      if (behandlingSkalSendeBrev(behandling)) {
        next()
      } else {
        setVisAttesteringsmodal(true)
      }
    })
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading size={'large'} level={'1'}>
            Beregning og vedtak
          </Heading>
        </HeadingWrapper>
        <InfoWrapper>
          <div className="text">
            Vilkårsresultat: <strong>{formaterVedtaksResultat(vedtaksresultat, virkningstidspunkt)}</strong>
          </div>
        </InfoWrapper>
        {!beregningFraState && !isFailure(beregning) && <Spinner visible label="Laster" />}
        {isFailure(beregning) && <ApiErrorAlert>Kunne ikke hente beregning</ApiErrorAlert>}
        {beregningFraState &&
          {
            [Beregningstype.BP]: <BarnepensjonSammendrag behandling={behandling} beregning={beregningFraState} />,
            [Beregningstype.OMS]: <OmstillingsstoenadSammendrag beregning={beregningFraState} />,
          }[beregningFraState.type]}
        {behandling.sakType === ISaksType.OMSTILLINGSSTOENAD && <Avkorting />}
      </ContentHeader>
      {behandles ? (
        <BehandlingHandlingKnapper>
          {isFailure(vedtak) && <ErrorMessage>Vedtaksoppdatering feilet</ErrorMessage>}
          {visAttesteringsmodal ? (
            <SendTilAttesteringModal />
          ) : (
            <Button loading={isPending(vedtak)} variant="primary" size="medium" onClick={opprettEllerOppdaterVedtak}>
              {behandlingSkalSendeBrev(behandling) ? 'Gå videre til brev' : 'Fatt vedtak'}
            </Button>
          )}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

const InfoWrapper = styled.div`
  margin-top: 1em;
  max-width: 500px;

  .text {
    margin: 1em 0 5em 0;
  }
`

const ApiErrorAlert = styled(Alert).attrs({ variant: 'error' })`
  margin-top: 8px;
`
