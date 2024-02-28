import { useKlage } from '~components/klage/useKlage'
import { BodyShort, Heading, Tag } from '@navikt/ds-react'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { KlageStatus, teksterKabalstatus, teksterKlagestatus } from '~shared/types/Klage'
import { tagColors, TagList } from '~shared/Tags'
import { formaterSakstype, formaterStringDato } from '~utils/formattering'
import { Info, Tekst } from '~components/behandling/attestering/styled'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import AvsluttKlage from '~components/klage/AvsluttKlage'
import React, { useEffect, useState } from 'react'
import { updateVedtakSammendrag } from '~store/reducers/VedtakReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { AttesteringEllerUnderkjenning } from '~components/behandling/attestering/attestering/attesteringEllerUnderkjenning'
import { IBeslutning } from '~components/behandling/attestering/types'
import { hentSaksbehandlerForReferanseOppgaveUnderArbeid } from '~shared/api/oppgaver'
import {
  resetSaksbehandlerGjeldendeOppgave,
  setSaksbehandlerGjeldendeOppgave,
} from '~store/reducers/SaksbehandlerGjeldendeOppgaveForBehandlingReducer'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export function KlageSidemeny() {
  const klage = useKlage()
  const dispatch = useAppDispatch()
  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [beslutning, setBeslutning] = useState<IBeslutning>()
  const [saksbehandlerForOppgaveResult, hentSaksbehandlerForOppgave] = useApiCall(
    hentSaksbehandlerForReferanseOppgaveUnderArbeid
  )
  const kanAttestere = !!klage && innloggetSaksbehandler.kanAttestere && klage.status === KlageStatus.FATTET_VEDTAK

  useEffect(() => {
    if (!klage) return
    fetchVedtakSammendrag(klage.id, (vedtakSammendrag, statusCode) => {
      if (statusCode === 200) {
        dispatch(updateVedtakSammendrag(vedtakSammendrag))
      }
    })
  }, [klage?.id])

  useEffect(() => {
    if (!klage) return
    hentSaksbehandlerForOppgave(
      { referanse: klage.id, sakId: klage.sak.id },
      (saksbehandler, statusCode) => {
        if (statusCode === 200) {
          dispatch(setSaksbehandlerGjeldendeOppgave(saksbehandler.ident))
        }
      },
      () => dispatch(resetSaksbehandlerGjeldendeOppgave())
    )
  }, [])

  if (!klage) {
    return (
      <Sidebar>
        <SidebarPanel></SidebarPanel>
      </Sidebar>
    )
  }

  return (
    <Sidebar>
      <SidebarPanel border>
        <Heading size="small">Klage</Heading>
        <Heading size="xsmall" spacing>
          {teksterKlagestatus[klage.status]}
        </Heading>

        {mapApiResult(
          fetchVedtakStatus,
          <Spinner label="Henter vedtaksdetaljer" visible />,
          () => (
            <ApiErrorAlert>Kunne ikke hente vedtak</ApiErrorAlert>
          ),
          (vedtak) => vedtak && <BodyShort>Vedtak er opprettet: {vedtak.vedtakType}</BodyShort>
        )}

        {klage.kabalStatus && (
          <>
            <Heading size="small">Status Kabal</Heading>
            <Heading size="xsmall">{teksterKabalstatus[klage.kabalStatus]}</Heading>
          </>
        )}

        <TagList>
          <li>
            <Tag variant={tagColors[klage.sak.sakType]}>{formaterSakstype(klage.sak.sakType)}</Tag>
          </li>
        </TagList>

        <div className="flex">
          <div>
            <Info>Klager</Info>
            <Tekst>{klage.innkommendeDokument?.innsender ?? 'Ukjent'}</Tekst>
          </div>
          <div>
            <Info>Klagedato</Info>
            <Tekst>
              {klage.innkommendeDokument?.mottattDato
                ? formaterStringDato(klage.innkommendeDokument.mottattDato)
                : 'Ukjent'}
            </Tekst>
          </div>
        </div>
      </SidebarPanel>
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
                erFattet={klage?.status === KlageStatus.FATTET_VEDTAK}
              />
            )
          )}
        </>
      )}
      {isFailureHandler({
        apiResult: saksbehandlerForOppgaveResult,
        errorMessage: 'Kunne ikke hente saksbehandler gjeldende oppgave. Husk å tildele oppgaven.',
      })}
      <DokumentlisteLiten fnr={klage.sak.ident} />
      <AvsluttKlage />
    </Sidebar>
  )
}
