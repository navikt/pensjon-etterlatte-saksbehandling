import { Content, ContentHeader } from '~shared/styled'
import { useEffect, useState } from 'react'
import { Alert, ErrorMessage, Heading } from '@navikt/ds-react'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { getData, hentVedtaksbrev, isSuccessOrNotFound, opprettVedtaksbrev } from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import { Soeknadsdato } from '../soeknadsoversikt/Soeknadsdato'
import styled from 'styled-components'
import { SendTilAttesteringModal } from '../handlinger/sendTilAttesteringModal'
import {
  behandlingErRedigerbar,
  behandlingSkalSendeBrev,
  sisteBehandlingHendelse,
} from '~components/behandling/felles/utils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import MottakerPanel from '~components/behandling/brev/detaljer/MottakerPanel'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import Spinner from '~shared/Spinner'
import { BrevProsessType, IBrev } from '~shared/types/Brev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { isFailure, isPending, isPendingOrInitial, useApiCall } from '~shared/hooks/useApiCall'

import { fattVedtak } from '~shared/api/vedtaksvurdering'
import { SjekklisteValideringErrorSummary } from '~components/behandling/sjekkliste/SjekklisteValideringErrorSummary'
import { IHendelse } from '~shared/types/IHendelse'
import { oppdaterBehandling, resetBehandling } from '~store/reducers/BehandlingReducer'
import { hentBehandling } from '~shared/api/behandling'
import { useAppDispatch } from '~store/Store'
import { getVergeadresseFraGrunnlag } from '~shared/api/grunnlag'
import { handleHentVergeadresseError } from '~components/person/Vergeadresse'

export const Vedtaksbrev = (props: { behandling: IDetaljertBehandling }) => {
  const { behandlingId } = useParams()
  const dispatch = useAppDispatch()
  const { sakId, soeknadMottattDato } = props.behandling
  const redigeres = behandlingErRedigerbar(props.behandling.status)

  const [vedtaksbrev, setVedtaksbrev] = useState<IBrev | undefined>(undefined)
  const [visAdvarselBehandlingEndret, setVisAdvarselBehandlingEndret] = useState(false)

  const [hentBrevStatus, hentBrev] = useApiCall(hentVedtaksbrev)
  const [opprettBrevStatus, opprettNyttVedtaksbrev] = useApiCall(opprettVedtaksbrev)
  const [, fetchBehandling] = useApiCall(hentBehandling)
  const [vergeadresse, getVergeadresse] = useApiCall(getVergeadresseFraGrunnlag)

  const behandlingRedigertEtterOpprettetBrev = (vedtaksbrev: IBrev, hendelser: IHendelse[]) => {
    if (!redigeres) {
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
  useEffect(() => {
    if (behandlingId && vedtaksbrev) {
      getVergeadresse(behandlingId)
    }
  }, [vedtaksbrev])

  useEffect(() => {
    if (
      !behandlingId ||
      !sakId ||
      !behandlingSkalSendeBrev(props.behandling.behandlingType, props.behandling.revurderingsaarsak)
    )
      return

    hentBrev(behandlingId, (brev, statusCode) => {
      if (statusCode === 200) {
        setVedtaksbrev(brev)
      } else if (statusCode === 204) {
        opprettNyttVedtaksbrev({ sakId, behandlingId }, (nyttBrev) => {
          setVedtaksbrev(nyttBrev)
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
                Vedtaksbrev
              </Heading>
            </HeadingWrapper>
            <Soeknadsdato mottattDato={soeknadMottattDato} />

            <br />
            {(vedtaksbrev?.prosessType === BrevProsessType.MANUELL ||
              vedtaksbrev?.prosessType === BrevProsessType.REDIGERBAR) && (
              <WarningAlert>
                {redigeres
                  ? 'Kan ikke generere brev automatisk. Du må selv redigere innholdet.'
                  : 'Dette er et manuelt opprettet brev. Kontroller innholdet nøye før attestering.'}
              </WarningAlert>
            )}
            {visAdvarselBehandlingEndret && (
              <WarningAlert>
                Behandling er redigert etter brevet ble opprettet. Gå gjennom brevet og vurder om det bør tilbakestilles
                for å få oppdaterte verdier fra behandlingen.
              </WarningAlert>
            )}
            <br />
            {vedtaksbrev && isSuccessOrNotFound(vergeadresse) && (
              <MottakerPanel
                vedtaksbrev={vedtaksbrev}
                oppdater={(val) => setVedtaksbrev({ ...vedtaksbrev, mottaker: val })}
                vergeadresse={getData(vergeadresse)}
                redigerbar={redigeres}
              />
            )}
          </ContentHeader>
        </Sidebar>

        {!!vedtaksbrev &&
          (vedtaksbrev?.prosessType === BrevProsessType.AUTOMATISK ? (
            <ForhaandsvisningBrev brev={vedtaksbrev} />
          ) : (
            <RedigerbartBrev
              brev={vedtaksbrev!!}
              kanRedigeres={redigeres}
              lukkAdvarselBehandlingEndret={lukkAdvarselBehandlingEndret}
            />
          ))}

        {isFailure(hentBrevStatus) && <ErrorMessage>Feil ved henting av brev</ErrorMessage>}
        {isFailure(opprettBrevStatus) && <ErrorMessage>Kunne ikke opprette brev</ErrorMessage>}
        {isFailure(vergeadresse) && handleHentVergeadresseError(vergeadresse)}
      </BrevContent>

      <Border />

      <SjekklisteValideringErrorSummary />

      <BehandlingHandlingKnapper>
        {redigeres && <SendTilAttesteringModal behandlingId={props.behandling.id} fattVedtakApi={fattVedtak} />}
      </BehandlingHandlingKnapper>
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

const WarningAlert = styled(Alert).attrs({ variant: 'warning' })`
  margin-bottom: 1em;
`
