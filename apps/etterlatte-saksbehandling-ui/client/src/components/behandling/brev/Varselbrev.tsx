import { Content, ContentHeader } from '~shared/styled'
import React, { useEffect, useState } from 'react'
import { Alert, Button, Heading } from '@navikt/ds-react'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { hentVedtaksbrev, opprettVedtaksbrev } from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import { Soeknadsdato } from '../soeknadsoversikt/Soeknadsdato'
import styled from 'styled-components'
import { behandlingErRedigerbar, behandlingSkalSendeBrev } from '~components/behandling/felles/utils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import Spinner from '~shared/Spinner'
import { IBrev } from '~shared/types/Brev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterBehandling, resetBehandling } from '~store/reducers/BehandlingReducer'
import { hentBehandling } from '~shared/api/behandling'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { isPending, isPendingOrInitial } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { BrevMottaker } from '~components/person/brev/mottaker/BrevMottaker'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import { handlinger } from '~components/behandling/handlinger/typer'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import NyttBrevHandlingerPanel from '~components/person/brev/NyttBrevHandlingerPanel'

export const FEATURE_TOGGLE_LAG_VARSELBREV = 'lag-varselbrev'

export const Varselbrev = (props: { behandling: IDetaljertBehandling }) => {
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const { sakId, soeknadMottattDato } = props.behandling
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const [redigerbar, setKanRedigeres] = useState(
    behandlingErRedigerbar(props.behandling.status) && innloggetSaksbehandler.skriveTilgang
  )

  const [varselbrev, setVarselbrev] = useState<IBrev>()
  const { next } = useBehandlingRoutes()

  const [hentBrevStatus, hentBrev] = useApiCall(hentVedtaksbrev)
  const [opprettBrevStatus, opprettNyttVarselbrev] = useApiCall(opprettVedtaksbrev)
  const [, fetchBehandling] = useApiCall(hentBehandling)

  const onSubmit = () => {
    next()
  }

  // Opppdaterer behandling for å sikre at siste hendelser er på plass
  useEffect(() => {
    if (behandlingId && varselbrev) {
      fetchBehandling(
        behandlingId,
        (behandling) => {
          dispatch(oppdaterBehandling(behandling))
        },
        () => dispatch(resetBehandling())
      )
    }
  }, [varselbrev])

  useEffect(() => {
    if (
      !behandlingId ||
      !sakId ||
      !behandlingSkalSendeBrev(props.behandling.behandlingType, props.behandling.revurderingsaarsak)
    )
      return

    hentBrev(behandlingId, (brev, statusCode) => {
      if (statusCode === 200) {
        setVarselbrev(brev)
      } else if (statusCode === 204) {
        opprettNyttVarselbrev({ sakId, behandlingId }, (nyttBrev) => {
          setVarselbrev(nyttBrev)
        })
      }
    })
  }, [behandlingId, sakId])

  if (isPendingOrInitial(hentBrevStatus)) {
    return <Spinner visible label="Henter brev ..." />
  } else if (isPending(opprettBrevStatus)) {
    return <Spinner visible label="Ingen brev funnet. Oppretter brev ..." />
  }

  return (
    <Content>
      <BrevContent>
        <Sidebar>
          <ContentHeader>
            <HeadingWrapper>
              <Heading spacing size="large" level="1">
                Varselbrev
              </Heading>
            </HeadingWrapper>
            <Soeknadsdato mottattDato={soeknadMottattDato} />

            {varselbrev && (
              <>
                <BrevTittel
                  brevId={varselbrev.id}
                  sakId={varselbrev.sakId}
                  tittel={varselbrev.tittel}
                  kanRedigeres={redigerbar}
                />
                <br />
                <BrevMottaker brev={varselbrev} kanRedigeres={redigerbar} />
              </>
            )}
          </ContentHeader>
        </Sidebar>

        {!!varselbrev && <RedigerbartBrev brev={varselbrev!!} kanRedigeres={redigerbar} />}

        {isFailureHandler({ apiResult: hentBrevStatus, errorMessage: 'Feil ved henting av brev' })}
        {isFailureHandler({ apiResult: opprettBrevStatus, errorMessage: 'Kunne ikke opprette brev' })}
      </BrevContent>

      <Border />

      {redigerbar ? (
        <BehandlingHandlingKnapper>
          <Button
            variant="primary"
            onClick={onSubmit}
            loading={isPending(hentBrevStatus) || isPending(opprettBrevStatus)}
          >
            {handlinger.NESTE.navn}
          </Button>
          {varselbrev && <NyttBrevHandlingerPanel brev={varselbrev} setKanRedigeres={setKanRedigeres} />}
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}

const BrevContent = styled.div`
  display: flex;
  height: 75vh;
  max-height: 75vh;
`

const Sidebar = styled.div`
  max-height: fit-content;
  min-width: 40%;
  width: 40%;
  border-right: 1px solid #c6c2bf;
`
styled(Alert).attrs({ variant: 'warning' })`
  margin-bottom: 1em;
`
