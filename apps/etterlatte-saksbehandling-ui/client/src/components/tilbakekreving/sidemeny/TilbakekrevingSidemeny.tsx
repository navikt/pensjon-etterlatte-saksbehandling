import { CollapsibleSidebar, SidebarContent, SidebarTools } from '~shared/styled'
import React, { useEffect, useState } from 'react'
import { BodyShort, Button } from '@navikt/ds-react'
import { ChevronLeftDoubleIcon, ChevronRightDoubleIcon } from '@navikt/aksel-icons'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import { SidebarPanel } from '~shared/components/Sidebar'
import { Dokumentoversikt } from '~components/person/dokumenter/dokumentoversikt'
import { IRolle } from '~store/reducers/SaksbehandlerReducer'
import { useAppSelector } from '~store/Store'
import { TilbakekrevingStatus } from '~shared/types/Tilbakekreving'
import { isFailure, isSuccess, Result } from '~shared/hooks/useApiCall'
import { isPending } from '@reduxjs/toolkit'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import { Attestering } from '~components/behandling/attestering/attestering/attestering'
import { IBeslutning } from '~components/behandling/attestering/types'
import { VedtakSammendrag, VedtakType } from '~components/vedtak/typer'

export function TilbakekrevingSidemeny() {
  const tilbakekreving = useTilbakekreving()
  //const vedtak = useVedtak() // TODO EY-2767
  const vedtak: VedtakSammendrag = {
    id: '9',
    behandlingId: 'd7497e08-2424-4275-a978-a0e55c51b229',
    vedtakType: VedtakType.TILBAKEKREVING,
    saksbehandlerId: '13018316042',
    datoFattet: '2023-10-04 15:51:51.732458+02',
  }
  //const dispatch = useAppDispatch()
  const saksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [collapsed, setCollapsed] = useState(false)

  //const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag) // TODO EY-2767
  const fetchVedtakStatus: Result<any> = {
    status: 'success',
    data: vedtak,
  }
  const [beslutning, setBeslutning] = useState<IBeslutning>()

  const kanAttestere =
    !!tilbakekreving &&
    saksbehandler.rolle === IRolle.attestant &&
    tilbakekreving?.status === TilbakekrevingStatus.FATTET_VEDTAK

  useEffect(() => {
    if (!tilbakekreving?.id) return
    /* TODO EY-2767
    fetchVedtakSammendrag(tilbakekreving.id, (vedtakSammendrag) => {
      if (vedtakSammendrag !== null) {
        dispatch(updateVedtakSammendrag(vedtakSammendrag))
      }
    })
    */
  }, [tilbakekreving?.id])

  return (
    <CollapsibleSidebar collapsed={collapsed}>
      <SidebarTools>
        <Button variant={collapsed ? 'primary' : 'tertiary'} onClick={() => setCollapsed((collapsed) => !collapsed)}>
          {collapsed ? <ChevronLeftDoubleIcon /> : <ChevronRightDoubleIcon />}
        </Button>
      </SidebarTools>
      <SidebarContent collapsed={collapsed}>
        <SidebarPanel>
          <BodyShort>Her kan vi vise info om tilbakekrevingen, som sakid: {tilbakekreving?.sak?.id}</BodyShort>
        </SidebarPanel>
      </SidebarContent>
      {kanAttestere && (
        <>
          {isFailure(fetchVedtakStatus) && <ApiErrorAlert>Kunne ikke hente vedtak</ApiErrorAlert>}
          {isPending(fetchVedtakStatus) && <Spinner label="Henter vedtaksdetaljer" visible />}
          {isSuccess(fetchVedtakStatus) && vedtak && (
            <Attestering
              setBeslutning={setBeslutning}
              beslutning={beslutning}
              behandlingId={tilbakekreving?.id}
              vedtak={vedtak}
              erFattet={tilbakekreving?.status === TilbakekrevingStatus.FATTET_VEDTAK}
            />
          )}
        </>
      )}

      {tilbakekreving?.sak.ident ? <Dokumentoversikt fnr={tilbakekreving.sak.ident} liten /> : null}
    </CollapsibleSidebar>
  )
}
