import { CollapsibleSidebar, SidebarContent, SidebarTools } from '~shared/styled'
import React, { useEffect, useState } from 'react'
import { BodyShort, Button } from '@navikt/ds-react'
import { ChevronLeftDoubleIcon, ChevronRightDoubleIcon } from '@navikt/aksel-icons'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import { SidebarPanel } from '~shared/components/Sidebar'
import { Dokumentoversikt } from '~components/person/dokumenter/dokumentoversikt'
import { IRolle } from '~store/reducers/InnloggetSaksbehandlerReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { TilbakekrevingStatus } from '~shared/types/Tilbakekreving'
import { isFailure, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { isPending } from '@reduxjs/toolkit'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import { AttesteringEllerUnderkjenning } from '~components/behandling/attestering/attestering/attesteringEllerUnderkjenning'
import { IBeslutning } from '~components/behandling/attestering/types'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { useVedtak } from '~components/vedtak/useVedtak'
import { updateVedtakSammendrag } from '~store/reducers/VedtakReducer'

export function TilbakekrevingSidemeny() {
  const tilbakekreving = useTilbakekreving()
  const vedtak = useVedtak()
  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [collapsed, setCollapsed] = useState(false)

  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)
  const [beslutning, setBeslutning] = useState<IBeslutning>()

  const kanAttestere =
    !!tilbakekreving &&
    innloggetSaksbehandler.rolle === IRolle.attestant &&
    tilbakekreving?.status === TilbakekrevingStatus.FATTET_VEDTAK

  useEffect(() => {
    if (!tilbakekreving?.id) return
    fetchVedtakSammendrag(tilbakekreving.id, (vedtakSammendrag) => {
      if (vedtakSammendrag !== null) {
        dispatch(updateVedtakSammendrag(vedtakSammendrag))
      }
    })
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
            <AttesteringEllerUnderkjenning
              setBeslutning={setBeslutning}
              beslutning={beslutning}
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
