import { Content, ContentHeader } from '~shared/styled'
import { useEffect, useState } from 'react'
import { Alert, Heading } from '@navikt/ds-react'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { hentVedtaksbrev, opprettVedtaksbrev } from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import { Soeknadsdato } from '../soeknadsoversikt/Soeknadsdato'
import styled from 'styled-components'
import { SendTilAttesteringModal } from '../handlinger/SendTilAttesteringModal'
import {
  behandlingErRedigerbar,
  behandlingSkalSendeBrev,
  sisteBehandlingHendelse,
  statusErRedigerbar,
} from '~components/behandling/felles/utils'
import {
  IBehandlingStatus,
  IBehandlingsType,
  IDetaljertBehandling,
  Vedtaksloesning,
} from '~shared/types/IDetaljertBehandling'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import Spinner from '~shared/Spinner'
import { BrevProsessType, IBrev } from '~shared/types/Brev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { useApiCall } from '~shared/hooks/useApiCall'

import { fattVedtak, upsertVedtak } from '~shared/api/vedtaksvurdering'
import { SjekklisteValideringErrorSummary } from '~components/behandling/sjekkliste/SjekklisteValideringErrorSummary'
import { IHendelse } from '~shared/types/IHendelse'
import { oppdaterBehandling, resetBehandling } from '~store/reducers/BehandlingReducer'
import { hentBehandling } from '~shared/api/behandling'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { isPending, isPendingOrInitial } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useSjekkliste, useSjekklisteValideringsfeil } from '~components/behandling/sjekkliste/useSjekkliste'
import { useBehandling } from '~components/behandling/useBehandling'
import { addValideringsfeil, Valideringsfeilkoder } from '~store/reducers/SjekklisteReducer'
import { visSjekkliste } from '~store/reducers/BehandlingSidemenyReducer'
import { BrevMottaker } from '~components/person/brev/mottaker/BrevMottaker'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'
import BrevutfallModal from '~components/behandling/brevutfall/BrevutfallModal'

export const Vedtaksbrev = (props: { behandling: IDetaljertBehandling }) => {
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const { sakId, soeknadMottattDato } = props.behandling
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const redigerbar = behandlingErRedigerbar(
    props.behandling.status,
    props.behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const [vedtaksbrev, setVedtaksbrev] = useState<IBrev>()
  const [visAdvarselBehandlingEndret, setVisAdvarselBehandlingEndret] = useState(false)

  const [hentBrevStatus, hentBrev] = useApiCall(hentVedtaksbrev)
  const [opprettBrevStatus, opprettNyttVedtaksbrev] = useApiCall(opprettVedtaksbrev)
  const [, fetchBehandling] = useApiCall(hentBehandling)

  const [visBrevutfall, setVisBrevutfall] = useState(true)

  const sjekkliste = useSjekkliste()
  const valideringsfeil = useSjekklisteValideringsfeil()
  const behandling = useBehandling()

  const behandlingRedigertEtterOpprettetBrev = (vedtaksbrev: IBrev, hendelser: IHendelse[]) => {
    if (!redigerbar) {
      return false
    }
    const hendelse = sisteBehandlingHendelse(hendelser)
    return new Date(hendelse.opprettet).getTime() > new Date(vedtaksbrev.statusEndret).getTime()
  }

  const lukkAdvarselBehandlingEndret = () => {
    setVisAdvarselBehandlingEndret(false)
  }

  // Opppdaterer behandling for å sikre at siste hendelser er på plass
  useEffect(() => {
    if (behandlingId && vedtaksbrev) {
      fetchBehandling(
        behandlingId,
        (behandling) => {
          dispatch(oppdaterBehandling(behandling))
          setVisAdvarselBehandlingEndret(behandlingRedigertEtterOpprettetBrev(vedtaksbrev, behandling.hendelser))
        },
        () => dispatch(resetBehandling())
      )
    }
  }, [vedtaksbrev])

  const hentBrevPaaNytt = () => {
    if (behandlingId) {
      hentBrev(behandlingId, (brev, statusCode) => {
        if (statusCode === 200) {
          setVedtaksbrev(brev)
        } else if (statusCode === 204) {
          opprettNyttVedtaksbrev({ sakId, behandlingId }, (nyttBrev) => {
            setVedtaksbrev(nyttBrev)
          })
        }
      })
    }
  }

  useEffect(() => {
    if (
      !behandlingId ||
      !sakId ||
      !behandlingSkalSendeBrev(props.behandling.behandlingType, props.behandling.revurderingsaarsak) ||
      (behandling?.kilde === Vedtaksloesning.GJENOPPRETTA && statusErRedigerbar(props.behandling.status))
    )
      return

    hentBrevPaaNytt()
  }, [behandlingId, sakId])

  const [, oppdaterVedtakRequest] = useApiCall(upsertVedtak)

  useEffect(() => {
    if (behandlingId) {
      if (props.behandling.kilde === Vedtaksloesning.GJENOPPRETTA && statusErRedigerbar(props.behandling.status)) {
        oppdaterVedtakRequest(props.behandling.id, () => {
          hentBrevPaaNytt()
        })
      }
    }
  }, [behandlingId])

  if (isPendingOrInitial(hentBrevStatus)) {
    return <Spinner visible label="Henter brev ..." />
  } else if (isPending(opprettBrevStatus)) {
    return <Spinner visible label="Ingen brev funnet. Oppretter brev ..." />
  }

  const kanSendeTilAttestering = (): boolean => {
    const erForestegangsbehandling = behandling?.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING
    if (erForestegangsbehandling) {
      const sjekklisteErBekreftet = sjekkliste !== null && sjekkliste.bekreftet
      if (!sjekklisteErBekreftet) {
        const fant = valideringsfeil.find((e) => e === Valideringsfeilkoder.MAA_HUKES_AV)
        if (!fant) {
          dispatch(addValideringsfeil(Valideringsfeilkoder.MAA_HUKES_AV))
        }
        dispatch(visSjekkliste())
      }
      return sjekklisteErBekreftet
    } else {
      return true
    }
  }

  return (
    <Content>
      {visBrevutfall && behandling && behandling.kilde === Vedtaksloesning.GJENOPPRETTA && (
        <BrevutfallModal behandling={behandling} onLagre={hentBrevPaaNytt} setVis={setVisBrevutfall} />
      )}
      <BrevContent>
        <Sidebar>
          <ContentHeader>
            <HeadingWrapper>
              <Heading spacing size="large" level="1">
                Vedtaksbrev
              </Heading>
            </HeadingWrapper>
            {soeknadMottattDato && <Soeknadsdato mottattDato={soeknadMottattDato} />}

            <br />
            {behandling?.status === IBehandlingStatus.FATTET_VEDTAK && (
              <WarningAlert>Kontroller innholdet nøye før attestering!</WarningAlert>
            )}

            {visAdvarselBehandlingEndret && (
              <WarningAlert>
                Behandling er redigert etter brevet ble opprettet. Gå gjennom brevet og vurder om det bør tilbakestilles
                for å få oppdaterte verdier fra behandlingen.
              </WarningAlert>
            )}

            {vedtaksbrev && (
              <>
                <BrevTittel
                  brevId={vedtaksbrev.id}
                  sakId={vedtaksbrev.sakId}
                  tittel={vedtaksbrev.tittel}
                  kanRedigeres={redigerbar}
                />
                <br />
                <BrevSpraak brev={vedtaksbrev} kanRedigeres={redigerbar} />
                <br />
                <BrevMottaker brev={vedtaksbrev} kanRedigeres={redigerbar} />
              </>
            )}
          </ContentHeader>
        </Sidebar>

        {!!vedtaksbrev &&
          (vedtaksbrev?.prosessType === BrevProsessType.AUTOMATISK ? (
            <ForhaandsvisningBrev brev={vedtaksbrev} />
          ) : (
            <RedigerbartBrev
              brev={vedtaksbrev!!}
              kanRedigeres={redigerbar}
              lukkAdvarselBehandlingEndret={lukkAdvarselBehandlingEndret}
            />
          ))}

        {isFailureHandler({ apiResult: hentBrevStatus, errorMessage: 'Feil ved henting av brev' })}
        {isFailureHandler({ apiResult: opprettBrevStatus, errorMessage: 'Kunne ikke opprette brev' })}
      </BrevContent>

      <Border />

      <SjekklisteValideringErrorSummary />

      <BehandlingHandlingKnapper>
        {redigerbar && (
          <SendTilAttesteringModal
            behandlingId={props.behandling.id}
            fattVedtakApi={fattVedtak}
            sakId={sakId}
            validerKanSendeTilAttestering={kanSendeTilAttestering}
          />
        )}
      </BehandlingHandlingKnapper>
    </Content>
  )
}

const BrevContent = styled.div`
  display: flex;
`

const Sidebar = styled.div`
  max-height: fit-content;
  min-width: 40%;
  width: 40%;
  border-right: 1px solid #c6c2bf;
`

const WarningAlert = styled(Alert).attrs({ variant: 'warning' })`
  margin-bottom: 1em;
`
