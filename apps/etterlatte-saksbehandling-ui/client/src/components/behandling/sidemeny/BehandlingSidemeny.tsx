import { Behandlingsoppsummering } from '~components/behandling/attestering/oppsummering/oppsummering'
import { AttesteringEllerUnderkjenning } from '~components/behandling/attestering/attestering/attesteringEllerUnderkjenning'
import AnnullerBehandling from '~components/behandling/handlinger/AnnullerBehanding'
import React, { useEffect, useState } from 'react'
import { IBeslutning } from '~components/behandling/attestering/types'
import { BehandlingFane, IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import {
  IBehandlingStatus,
  IBehandlingsType,
  IDetaljertBehandling,
  UtlandstilknytningType,
} from '~shared/types/IDetaljertBehandling'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { IHendelseType } from '~shared/types/IHendelse'
import { Sidebar } from '~shared/components/Sidebar'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import { useVedtak } from '~components/vedtak/useVedtak'
import { VedtakSammendrag } from '~components/vedtak/typer'
import { updateVedtakSammendrag } from '~store/reducers/VedtakReducer'
import { Tabs } from '@navikt/ds-react'
import { ClockDashedIcon, DocPencilIcon, FileTextIcon } from '@navikt/aksel-icons'
import { Sjekkliste } from '~components/behandling/sjekkliste/Sjekkliste'
import { useSelectorBehandlingSidemenyFane } from '~components/behandling/sidemeny/useSelectorBehandlingSidemeny'
import { visFane } from '~store/reducers/BehandlingSidemenyReducer'
import { updateSjekkliste } from '~store/reducers/SjekklisteReducer'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import { hentSjekkliste, opprettSjekkliste } from '~shared/api/sjekkliste'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

import { isInitial, isPending, mapApiResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { useOppgaveUnderBehandling } from '~shared/hooks/useOppgaveUnderBehandling'
import { OppgaveEndring } from './OppgaveEndring'
import { NotatPanel } from '~components/behandling/sidemeny/NotatPanel'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'

const finnUtNasjonalitet = (behandling: IBehandlingReducer): UtlandstilknytningType | null => {
  if (behandling.utlandstilknytning?.type) {
    return behandling.utlandstilknytning?.type
  } else {
    return null
  }
}
const mapTilBehandlingInfo = (behandling: IBehandlingReducer, vedtak: VedtakSammendrag | null): IBehandlingInfo => ({
  type: behandling.behandlingType,
  behandlingId: behandling.id,
  sakId: behandling.sakId,
  sakType: behandling.sakType,
  sakEnhetId: behandling.sakEnhetId,
  nasjonalEllerUtland: finnUtNasjonalitet(behandling),
  status: behandling.status,
  kilde: behandling.kilde,
  behandlendeSaksbehandler: vedtak?.behandlendeSaksbehandler,
  attesterendeSaksbehandler: vedtak?.attesterendeSaksbehandler,
  virkningsdato: behandling.virkningstidspunkt?.dato,
  datoFattet: vedtak?.datoFattet,
  datoAttestert: vedtak?.datoAttestert,
  underkjentLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_UNDERKJENT),
  fattetLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_FATTET),
  attestertLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_ATTESTERT),
})

export const BehandlingSidemeny = ({ behandling }: { behandling: IBehandlingReducer }) => {
  const soeker = usePersonopplysninger()?.soeker?.opplysning
  const vedtak = useVedtak()
  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)
  const [beslutning, setBeslutning] = useState<IBeslutning>()
  const fane = useSelectorBehandlingSidemenyFane()

  const skalViseNotater = useFeatureEnabledMedDefault('notater', false)
  const [oppgaveResult] = useOppgaveUnderBehandling({ referanse: behandling.id })

  const behandlingsinfo = mapTilBehandlingInfo(behandling, vedtak)

  const kanAttestere =
    behandling && innloggetSaksbehandler.kanAttestere && behandlingsinfo?.status === IBehandlingStatus.FATTET_VEDTAK

  const [hentSjekklisteResult, hentSjekklisteForBehandling, resetSjekklisteResult] = useApiCall(hentSjekkliste)
  const [opprettSjekklisteResult, opprettSjekklisteForBehandling, resetOpprettSjekkliste] =
    useApiCall(opprettSjekkliste)

  const skalViseSjekkliste = (behandling: IDetaljertBehandling): boolean => {
    return (
      behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING ||
      behandling.behandlingType === IBehandlingsType.REVURDERING
    )
  }

  useEffect(() => {
    fetchVedtakSammendrag(behandling.id, (vedtakSammendrag) => {
      dispatch(updateVedtakSammendrag(vedtakSammendrag))
    })
  }, [])

  useEffect(() => {
    resetSjekklisteResult()
    resetOpprettSjekkliste()
    if (behandling && skalViseSjekkliste(behandling) && isInitial(hentSjekklisteResult)) {
      hentSjekklisteForBehandling(behandling.id, (result, statusCode) => {
        if (statusCode === 204) {
          if (!erFerdigBehandlet(behandling.status)) {
            opprettSjekklisteForBehandling(behandling.id, (nySjekkliste) => {
              dispatch(updateSjekkliste(nySjekkliste))
            })
          }
        } else {
          dispatch(updateSjekkliste(result))
        }
      })
    }
  }, [])

  return (
    <Sidebar>
      {behandlingsinfo && (
        <>
          <Behandlingsoppsummering
            behandlingsInfo={behandlingsinfo}
            beslutning={beslutning}
            behandlendeSaksbehandler={vedtak?.behandlendeSaksbehandler}
          />
          {kanAttestere && (
            <>
              {mapApiResult(
                fetchVedtakStatus,
                <Spinner label="Henter vedtaksdetaljer" />,
                () => (
                  <ApiErrorAlert>Kunne ikke hente vedtak</ApiErrorAlert>
                ),
                (vedtak) => (
                  <AttesteringEllerUnderkjenning
                    setBeslutning={setBeslutning}
                    beslutning={beslutning}
                    vedtak={vedtak}
                    erFattet={behandling.status === IBehandlingStatus.FATTET_VEDTAK}
                  />
                )
              )}
            </>
          )}
        </>
      )}

      {isFailureHandler({
        apiResult: opprettSjekklisteResult,
        errorMessage: 'Opprettelsen av sjekkliste feilet',
      })}

      {isFailureHandler({
        apiResult: oppgaveResult,
        errorMessage: 'Kunne ikke hente saksbehandler gjeldende oppgave. Husk å tildele oppgaven.',
      })}

      <Spinner label="Henter saksbehandler for oppgave" visible={isPending(oppgaveResult)} />

      <Tabs value={fane} iconPosition="top" onChange={(val) => dispatch(visFane(val as BehandlingFane))}>
        <Tabs.List>
          <Tabs.Tab value={BehandlingFane.DOKUMENTER} label="Dokumenter" icon={<FileTextIcon title="dokumenter" />} />
          {skalViseSjekkliste(behandling) && (
            <Tabs.Tab
              value={BehandlingFane.SJEKKLISTE}
              label="Sjekkliste"
              icon={<DocPencilIcon title="sjekkliste" />}
            />
          )}
          <Tabs.Tab value={BehandlingFane.HISTORIKK} label="Historikk" icon={<ClockDashedIcon />} />
        </Tabs.List>

        <Tabs.Panel value={BehandlingFane.DOKUMENTER}>
          {soeker?.foedselsnummer && (
            <>
              {skalViseNotater && (
                <NotatPanel sakId={behandling.sakId} behandlingId={behandling.id} fnr={soeker?.foedselsnummer} />
              )}
              <DokumentlisteLiten fnr={soeker.foedselsnummer} />
            </>
          )}
        </Tabs.Panel>
        <Tabs.Panel value={BehandlingFane.HISTORIKK}>
          <OppgaveEndring oppgaveResult={oppgaveResult} />
        </Tabs.Panel>

        {skalViseSjekkliste(behandling) && (
          <Tabs.Panel value={BehandlingFane.SJEKKLISTE}>
            <Sjekkliste behandling={behandling} />

            <Spinner label="Henter sjekkliste ..." visible={isPending(hentSjekklisteResult)} />

            {!erFerdigBehandlet(behandling.status) &&
              isFailureHandler({
                apiResult: hentSjekklisteResult,
                errorMessage: 'Kunne ikke hente konfigurasjonsverdier',
              })}
          </Tabs.Panel>
        )}
      </Tabs>
      <AnnullerBehandling behandlingType={behandling.behandlingType} />
    </Sidebar>
  )
}
