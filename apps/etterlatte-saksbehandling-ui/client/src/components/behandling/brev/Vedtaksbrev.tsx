import React, { useEffect, useState } from 'react'
import { Alert, Box, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { hentBrevTilBehandling, opprettBrevTilBehandling } from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { SendTilAttesteringModal } from '../handlinger/SendTilAttesteringModal'
import {
  behandlingErRedigerbar,
  sisteBehandlingHendelse,
  statusErRedigerbar,
} from '~components/behandling/felles/utils'
import { IBehandlingStatus, IDetaljertBehandling, Vedtaksloesning } from '~shared/types/IDetaljertBehandling'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import Spinner from '~shared/Spinner'
import { BrevProsessType, IBrev } from '~shared/types/Brev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { fattVedtak, upsertVedtak } from '~shared/api/vedtaksvurdering'
import { SjekklisteValideringErrorSummary } from '~components/behandling/sjekkliste/SjekklisteValideringErrorSummary'
import { IHendelse } from '~shared/types/IHendelse'
import { oppdaterDetaljertBehandling, resetBehandling } from '~store/reducers/BehandlingReducer'
import { hentBehandling } from '~shared/api/behandling'
import { useAppDispatch } from '~store/Store'
import { isPending, isPendingOrInitial } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useSjekkliste, useSjekklisteValideringsfeil } from '~components/behandling/sjekkliste/useSjekkliste'
import { useBehandling } from '~components/behandling/useBehandling'
import { addValideringsfeil, Valideringsfeilkoder } from '~store/reducers/SjekklisteReducer'
import { visSjekkliste } from '~store/reducers/BehandlingSidemenyReducer'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'
import BrevutfallModal from '~components/behandling/brevutfall/BrevutfallModal'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { ApiErrorAlert } from '~ErrorBoundary'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { BrevMottakerWrapper } from '~components/person/brev/mottaker/BrevMottakerWrapper'
import { formaterDato } from '~utils/formatering/dato'

export const Vedtaksbrev = (props: { behandling: IDetaljertBehandling }) => {
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const { sakId, soeknadMottattDato } = props.behandling
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const redigerbar = behandlingErRedigerbar(
    props.behandling.status,
    props.behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const [vedtaksbrev, setVedtaksbrev] = useState<IBrev>()
  const [visAdvarselBehandlingEndret, setVisAdvarselBehandlingEndret] = useState(false)
  const [visAdvarselIngenAvdoede, setVisAdvarselIngenAvdoede] = useState(false)

  const [hentBrevStatus, hentBrev] = useApiCall(hentBrevTilBehandling)
  const [opprettBrevStatus, opprettNyttVedtaksbrev] = useApiCall(opprettBrevTilBehandling)
  const [, fetchBehandling] = useApiCall(hentBehandling)

  const [visBrevutfall, setVisBrevutfall] = useState(true)

  const [avbruttUtenBrev, setAvbruttUtenBrev] = useState(false)

  const sjekkliste = useSjekkliste()
  const valideringsfeil = useSjekklisteValideringsfeil()
  const behandling = useBehandling()
  const personopplysninger = usePersonopplysninger()
  const [tilbakestilt, setTilbakestilt] = useState(false)

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
          dispatch(oppdaterDetaljertBehandling(behandling))
          setVisAdvarselBehandlingEndret(behandlingRedigertEtterOpprettetBrev(vedtaksbrev, behandling.hendelser))
          setVisAdvarselIngenAvdoede(redigerbar && personopplysninger?.avdoede.length === 0)
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
        } else if (statusCode === 204 && redigerbar) {
          opprettNyttVedtaksbrev({ sakId, behandlingId }, (nyttBrev) => {
            setVedtaksbrev(nyttBrev)
          })
        } else {
          if (behandling?.status === IBehandlingStatus.AVBRUTT) {
            setAvbruttUtenBrev(true)
          }
        }
      })
    }
  }

  useEffect(() => {
    if (
      !behandlingId ||
      !sakId ||
      !props.behandling.sendeBrev ||
      (behandling?.kilde === Vedtaksloesning.GJENOPPRETTA && statusErRedigerbar(props.behandling.status))
    )
      return

    hentBrevPaaNytt()
  }, [behandlingId, sakId, tilbakestilt])

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
    return <Spinner label="Henter brev ..." />
  } else if (isPending(opprettBrevStatus)) {
    return <Spinner label="Ingen brev funnet. Oppretter brev ..." />
  }

  if (avbruttUtenBrev) {
    return <ApiErrorAlert>Behandlingen er avbrutt og ingen brev finnes</ApiErrorAlert>
  }

  const kanSendeTilAttestering = (): boolean => {
    const sjekklisteErBekreftet = sjekkliste !== null && sjekkliste.bekreftet
    if (!sjekklisteErBekreftet) {
      const fant = valideringsfeil.find((e) => e === Valideringsfeilkoder.MAA_HUKES_AV)
      if (!fant) {
        dispatch(addValideringsfeil(Valideringsfeilkoder.MAA_HUKES_AV))
      }
      dispatch(visSjekkliste())
    }
    return sjekklisteErBekreftet
  }

  return (
    <>
      {behandling?.status &&
        statusErRedigerbar(behandling?.status) &&
        visBrevutfall &&
        behandling &&
        behandling.kilde === Vedtaksloesning.GJENOPPRETTA && (
          <BrevutfallModal behandling={behandling} onLagre={hentBrevPaaNytt} setVis={setVisBrevutfall} />
        )}
      <HStack wrap={false}>
        <SidebarBox paddingInline="space-16" paddingBlock="space-16 space-4">
          <VStack gap="space-4">
            <Heading spacing size="large" level="1">
              Vedtaksbrev
            </Heading>

            {soeknadMottattDato && <Label>Søknad mottatt: {formaterDato(soeknadMottattDato)}</Label>}

            <br />
            {behandling?.status === IBehandlingStatus.FATTET_VEDTAK && (
              <Alert variant="warning">Kontroller innholdet nøye før attestering!</Alert>
            )}

            {visAdvarselBehandlingEndret && (
              <Alert variant="warning">
                Behandling er redigert etter brevet ble opprettet. Gå gjennom brevet og vurder om det bør tilbakestilles
                for å få oppdaterte verdier fra behandlingen.
              </Alert>
            )}

            {visAdvarselIngenAvdoede && (
              <Alert variant="warning">Behandlingen har ingen avdøde. Se nøye gjennom brevet med tanke på dette.</Alert>
            )}

            {vedtaksbrev && (
              <>
                <BrevTittel
                  brevId={vedtaksbrev.id}
                  sakId={vedtaksbrev.sakId}
                  tittel={vedtaksbrev.tittel}
                  kanRedigeres={redigerbar}
                />
                <BrevSpraak brev={vedtaksbrev} kanRedigeres={redigerbar} />

                <BrevMottakerWrapper brev={vedtaksbrev} kanRedigeres={redigerbar} />
              </>
            )}
          </VStack>
        </SidebarBox>

        {!!vedtaksbrev &&
          (vedtaksbrev?.prosessType === BrevProsessType.AUTOMATISK ? (
            <ForhaandsvisningBrev brev={vedtaksbrev} />
          ) : (
            <RedigerbartBrev
              brev={vedtaksbrev!!}
              kanRedigeres={redigerbar}
              lukkAdvarselBehandlingEndret={lukkAdvarselBehandlingEndret}
              tilbakestillingsaction={() => {
                setTilbakestilt(true)
              }}
            />
          ))}

        {isFailureHandler({ apiResult: hentBrevStatus, errorMessage: 'Feil ved henting av brev' })}
        {isFailureHandler({ apiResult: opprettBrevStatus, errorMessage: 'Kunne ikke opprette brev' })}
      </HStack>

      <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0">
        <SjekklisteValideringErrorSummary />

        <BehandlingHandlingKnapper>
          {redigerbar && (
            <SendTilAttesteringModal
              behandlingId={props.behandling.id}
              fattVedtakApi={fattVedtak}
              validerKanSendeTilAttestering={kanSendeTilAttestering}
            />
          )}
        </BehandlingHandlingKnapper>
      </Box>
    </>
  )
}

const SidebarBox = styled(Box)`
  max-height: fit-content;
  min-width: 40%;
  width: 40%;
  border-right: 1px solid var(--a-border-subtle);
`
