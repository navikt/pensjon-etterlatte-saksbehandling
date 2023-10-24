import { Behandlingsoppsummering } from '~components/behandling/attestering/oppsummering/oppsummering'
import { Attestering } from '~components/behandling/attestering/attestering/attestering'
import { Dokumentoversikt } from '~components/person/dokumenter/dokumentoversikt'
import AnnullerBehandling from '~components/behandling/handlinger/AnnullerBehanding'
import { useEffect, useState } from 'react'
import { IBeslutning } from '~components/behandling/attestering/types'
import { BehandlingFane, IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { IRolle } from '~store/reducers/SaksbehandlerReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { useBehandling } from '~components/behandling/useBehandling'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
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
import { DocPencilIcon, FileTextIcon } from '@navikt/aksel-icons'
import { Sjekkliste } from '~components/behandling/sjekkliste/Sjekkliste'
import { useBehandlingSidemenyFane } from '~components/behandling/sidemeny/useBehandlingSidemeny'
import { visFane } from '~store/reducers/BehandlingSidemenyReducer'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { featureToggleSjekklisteAktivert } from '~shared/types/Sjekkliste'

const mapTilBehandlingInfo = (behandling: IBehandlingReducer, vedtak: VedtakSammendrag | null): IBehandlingInfo => ({
  type: behandling.behandlingType,
  behandlingId: behandling.id,
  sakId: behandling.sakId,
  sakType: behandling.sakType,
  status: behandling.status,
  saksbehandler: vedtak?.saksbehandlerId,
  virkningsdato: behandling.virkningstidspunkt?.dato,
  datoFattet: vedtak?.datoFattet,
  datoAttestert: vedtak?.datoAttestert,
  underkjentLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_UNDERKJENT),
  fattetLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_FATTET),
  attestertLogg: behandling.hendelser.filter((hendelse) => hendelse.hendelse === IHendelseType.VEDTAK_ATTESTERT),
})

export const BehandlingSidemeny = () => {
  const behandling = useBehandling()
  const vedtak = useVedtak()
  const dispatch = useAppDispatch()
  const saksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)
  const [beslutning, setBeslutning] = useState<IBeslutning>()
  const fane = useBehandlingSidemenyFane()
  const sjekklisteAktivert = useFeatureEnabledMedDefault(featureToggleSjekklisteAktivert, false)

  const behandlingsinfo = behandling ? mapTilBehandlingInfo(behandling, vedtak) : undefined

  const kanAttestere =
    !!behandling &&
    saksbehandler.rolle === IRolle.attestant &&
    behandlingsinfo?.status === IBehandlingStatus.FATTET_VEDTAK

  useEffect(() => {
    if (!behandling?.id) return

    fetchVedtakSammendrag(behandling.id, (vedtakSammendrag) => {
      if (vedtakSammendrag !== null) {
        dispatch(updateVedtakSammendrag(vedtakSammendrag))
      }
    })
  }, [behandling?.id])

  return (
    <Sidebar>
      {behandlingsinfo && (
        <>
          <Behandlingsoppsummering behandlingsInfo={behandlingsinfo} beslutning={beslutning} />

          {kanAttestere && (
            <>
              {isFailure(fetchVedtakStatus) && <ApiErrorAlert>Kunne ikke hente vedtak</ApiErrorAlert>}
              {isPending(fetchVedtakStatus) && <Spinner label="Henter vedtaksdetaljer" visible />}
              {isSuccess(fetchVedtakStatus) && vedtak && (
                <Attestering
                  setBeslutning={setBeslutning}
                  beslutning={beslutning}
                  behandlingId={behandling?.id}
                  vedtak={vedtak}
                  erFattet={behandling?.status === IBehandlingStatus.FATTET_VEDTAK}
                />
              )}
            </>
          )}
        </>
      )}

      {sjekklisteAktivert && (
        <Tabs value={fane} iconPosition="top" onChange={(val) => dispatch(visFane(val as BehandlingFane))}>
          <Tabs.List>
            <Tabs.Tab value={BehandlingFane.DOKUMENTER} label="Dokumenter" icon={<FileTextIcon title="dokumenter" />} />
            <Tabs.Tab
              value={BehandlingFane.SJEKKLISTE}
              label="Sjekkliste"
              icon={<DocPencilIcon title="sjekkliste" />}
            />
          </Tabs.List>
          <Tabs.Panel value={BehandlingFane.DOKUMENTER}>
            {behandling?.søker?.foedselsnummer && <Dokumentoversikt fnr={behandling.søker.foedselsnummer} liten />}
          </Tabs.Panel>
          <Tabs.Panel value={BehandlingFane.SJEKKLISTE}>
            {behandling?.søker?.foedselsnummer && <Sjekkliste behandling={behandling} />}
          </Tabs.Panel>
        </Tabs>
      )}

      {!sjekklisteAktivert && behandling?.søker?.foedselsnummer && (
        <Dokumentoversikt fnr={behandling.søker.foedselsnummer} liten />
      )}

      <AnnullerBehandling />
    </Sidebar>
  )
}
