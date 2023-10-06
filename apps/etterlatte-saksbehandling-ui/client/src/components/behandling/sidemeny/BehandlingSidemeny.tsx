import { Behandlingsoppsummering } from '~components/behandling/attestering/oppsummering/oppsummering'
import { Attestering } from '~components/behandling/attestering/attestering/attestering'
import { Dokumentoversikt } from '~components/person/dokumenter/dokumentoversikt'
import AnnullerBehandling from '~components/behandling/handlinger/AnnullerBehanding'
import { useEffect, useState } from 'react'
import { IBeslutning } from '~components/behandling/attestering/types'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
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

      {behandling?.søker?.foedselsnummer && <Dokumentoversikt fnr={behandling.søker.foedselsnummer} liten />}

      <AnnullerBehandling />
    </Sidebar>
  )
}
