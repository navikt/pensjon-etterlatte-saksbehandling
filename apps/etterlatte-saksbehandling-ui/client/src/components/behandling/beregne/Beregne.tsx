import { Content, ContentHeader } from '~shared/styled'
import { TypeStatusWrap } from '../soeknadsoversikt/styled'
import { hentBehandlesFraStatus } from '../felles/utils'
import { formaterStringDato } from '~utils/formattering'
import { formaterVedtaksResultat, useVedtaksResultat } from '../useVedtaksResultat'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { useEffect, useState } from 'react'
import { hentBeregning } from '~shared/api/beregning'
import { oppdaterBeregning, resetBeregning } from '~store/reducers/BehandlingReducer'
import Spinner from '~shared/Spinner'
import { RequestStatus } from '~components/behandling/vilkaarsvurdering/utils'
import { Sammendrag } from '~components/behandling/beregne/Sammendrag'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { Button } from '@navikt/ds-react'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import styled from 'styled-components'

export const Beregne = () => {
  const { next } = useBehandlingRoutes()
  const behandling = useAppSelector((state) => state.behandlingReducer.behandling)
  const dispatch = useAppDispatch()
  const [status, setStatus] = useState<RequestStatus>(RequestStatus.notStarted)

  const laster = status === RequestStatus.notStarted || status === RequestStatus.isloading
  const ok = status === RequestStatus.ok
  const error = status === RequestStatus.error

  useEffect(() => {
    const fetchBeregning = async () => {
      setStatus(RequestStatus.isloading)
      const response = await hentBeregning(behandling.id)

      if (response.status === 'ok') {
        dispatch(oppdaterBeregning(response.data))
        setStatus(RequestStatus.ok)
      } else {
        dispatch(resetBeregning())
        setStatus(RequestStatus.error)
      }
    }
    fetchBeregning().catch(() => setStatus(RequestStatus.error))
  }, [])

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const vedtaksresultat = useVedtaksResultat()

  return (
    <Content>
      <ContentHeader>
        <h1>Beregning og vedtak</h1>
        <InfoWrapper>
          <DetailWrapper>
            <TypeStatusWrap type="barn">Barnepensjon</TypeStatusWrap>
            <TypeStatusWrap type="statsborgerskap">Nasjonal sak</TypeStatusWrap>
          </DetailWrapper>

          <div className="text">
            Vilkårsresultat: <strong>{formaterVedtaksResultat(vedtaksresultat, virkningstidspunkt)}</strong>
          </div>
        </InfoWrapper>
        {laster ? (
          <Spinner visible label="Laster" />
        ) : ok ? (
          <Sammendrag beregning={behandling.beregning!!} soeker={behandling.søker} />
        ) : error ? (
          'Fant ingen beregning'
        ) : (
          'Noe uventet har skjedd'
        )}
      </ContentHeader>
      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" size="medium" onClick={next}>
            Gå videre til brev
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

export const InfoWrapper = styled.div`
  margin-top: 5em;
  max-width: 500px;
  .text {
    margin: 2em 0 5em 0;
  }
`
export const DetailWrapper = styled.div`
  display: flex;
  max-width: 400px;
  margin-left: 0;
`
