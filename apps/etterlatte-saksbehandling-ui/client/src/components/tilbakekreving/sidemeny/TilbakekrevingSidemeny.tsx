import { CollapsibleSidebar, SidebarContent, SidebarTools } from '~shared/styled'
import React, { useEffect, useState } from 'react'
import { Alert, Button, Heading, Tag } from '@navikt/ds-react'
import { ChevronLeftDoubleIcon, ChevronRightDoubleIcon } from '@navikt/aksel-icons'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { useAppDispatch } from '~store/Store'
import { teksterTilbakekrevingStatus, TilbakekrevingStatus } from '~shared/types/Tilbakekreving'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import { AttesteringEllerUnderkjenning } from '~components/behandling/attestering/attestering/attesteringEllerUnderkjenning'
import { IBeslutning } from '~components/behandling/attestering/types'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { updateVedtakSammendrag } from '~store/reducers/VedtakReducer'

import { mapApiResult } from '~shared/api/apiUtils'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import { tagColors, TagList } from '~shared/Tags'
import { formaterSakstype } from '~utils/formattering'
import { Info, Tekst } from '~components/behandling/attestering/styled'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { SettPaaVent } from '~components/behandling/sidemeny/SettPaaVent'
import { useOppgaveUnderBehandling } from '~shared/hooks/useOppgaveUnderBehandling'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useSelectorOppgaveUnderBehandling } from '~store/selectors/useSelectorOppgaveUnderBehandling'

export function TilbakekrevingSidemeny() {
  const dispatch = useAppDispatch()

  const tilbakekreving = useTilbakekreving()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const oppgave = useSelectorOppgaveUnderBehandling()

  const [collapsed, setCollapsed] = useState(false)
  const [beslutning, setBeslutning] = useState<IBeslutning>()

  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)

  const kanAttestere =
    !!tilbakekreving &&
    innloggetSaksbehandler.kanAttestere &&
    tilbakekreving?.status === TilbakekrevingStatus.FATTET_VEDTAK

  useEffect(() => {
    if (!tilbakekreving?.id) return
    fetchVedtakSammendrag(tilbakekreving.id, (vedtakSammendrag, statusCode) => {
      if (statusCode === 200) {
        dispatch(updateVedtakSammendrag(vedtakSammendrag))
      }
    })
  }, [tilbakekreving?.id])

  if (!tilbakekreving) {
    return (
      <Sidebar>
        <SidebarPanel></SidebarPanel>
      </Sidebar>
    )
  }

  const [oppgaveResult] = useOppgaveUnderBehandling({ referanse: tilbakekreving.id })

  return (
    <CollapsibleSidebar collapsed={collapsed}>
      <SidebarTools>
        <Button variant={collapsed ? 'primary' : 'tertiary'} onClick={() => setCollapsed((collapsed) => !collapsed)}>
          {collapsed ? <ChevronLeftDoubleIcon /> : <ChevronRightDoubleIcon />}
        </Button>
      </SidebarTools>
      <SidebarContent collapsed={collapsed}>
        <SidebarPanel border>
          <Heading size="small">Tilbakekreving</Heading>
          <Heading size="xsmall" spacing>
            {teksterTilbakekrevingStatus[tilbakekreving!!.status]}
          </Heading>
          <TagList>
            <li>
              <Tag variant={tagColors[tilbakekreving!!.sak.sakType]}>
                {formaterSakstype(tilbakekreving!!.sak.sakType)}
              </Tag>
            </li>
          </TagList>
          <div className="info">
            <Info>Saksbehandler</Info>
            {mapApiResult(
              oppgaveResult,
              <Spinner visible={true} label="Henter oppgave" />,
              () => (
                <ApiErrorAlert>Kunne ikke hente saksbehandler fra oppgave</ApiErrorAlert>
              ),
              (oppgave) =>
                !!oppgave?.saksbehandler ? (
                  <Tekst>{oppgave.saksbehandler?.navn || oppgave.saksbehandler?.ident}</Tekst>
                ) : (
                  <Alert size="small" variant="warning">
                    Ingen saksbehandler har tatt denne oppgaven
                  </Alert>
                )
            )}
          </div>
          <div>
            <Info>Sakid:</Info>
            <KopierbarVerdi value={tilbakekreving!!.sak.id.toString()} />
          </div>

          <SettPaaVent oppgave={oppgave} redigerbar={true} />
        </SidebarPanel>
      </SidebarContent>
      {kanAttestere && (
        <>
          {mapApiResult(
            fetchVedtakStatus,
            <Spinner label="Henter vedtaksdetaljer" visible />,
            () => (
              <ApiErrorAlert>Kunne ikke hente vedtak</ApiErrorAlert>
            ),
            (vedtak) => (
              <AttesteringEllerUnderkjenning
                setBeslutning={setBeslutning}
                beslutning={beslutning}
                vedtak={vedtak}
                erFattet={tilbakekreving?.status === TilbakekrevingStatus.FATTET_VEDTAK}
              />
            )
          )}
        </>
      )}

      {tilbakekreving?.sak.ident && <DokumentlisteLiten fnr={tilbakekreving.sak.ident} />}
    </CollapsibleSidebar>
  )
}
