import { useKlage } from '~components/klage/useKlage'
import { Heading, HStack, VStack } from '@navikt/ds-react'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { KlageStatus, teksterKabalstatus, teksterKlagestatus } from '~shared/types/Klage'
import { formaterStringDato } from '~utils/formattering'
import { Info, Tekst } from '~components/behandling/attestering/styled'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import AvsluttKlage from '~components/klage/AvsluttKlage'
import React, { useEffect, useState } from 'react'
import { updateVedtakSammendrag } from '~store/reducers/VedtakReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { useAppDispatch } from '~store/Store'
import { mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { AttesteringEllerUnderkjenning } from '~components/behandling/attestering/attestering/attesteringEllerUnderkjenning'
import { IBeslutning } from '~components/behandling/attestering/types'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useOppgaveUnderBehandling } from '~shared/hooks/useOppgaveUnderBehandling'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { SakTypeTag } from '~shared/tags/SakTypeTag'

export function KlageSidemeny() {
  const klage = useKlage()
  const dispatch = useAppDispatch()
  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [beslutning, setBeslutning] = useState<IBeslutning>()
  const kanAttestere = !!klage && innloggetSaksbehandler.kanAttestere && klage.status === KlageStatus.FATTET_VEDTAK

  useEffect(() => {
    if (!klage) return
    fetchVedtakSammendrag(klage.id, (vedtakSammendrag, statusCode) => {
      if (statusCode === 200) {
        dispatch(updateVedtakSammendrag(vedtakSammendrag))
      }
    })
  }, [klage?.id, klage?.status])

  if (!klage) {
    return (
      <Sidebar>
        <SidebarPanel></SidebarPanel>
      </Sidebar>
    )
  }
  const [oppgaveResult] = useOppgaveUnderBehandling({ referanse: klage.id })

  return (
    <Sidebar>
      <SidebarPanel $border>
        <Heading size="small">Klage</Heading>
        <Heading size="xsmall" spacing>
          {teksterKlagestatus[klage.status]}
        </Heading>

        {klage.kabalStatus && (
          <>
            <Heading size="small">Status Kabal</Heading>
            <Heading size="xsmall">{teksterKabalstatus[klage.kabalStatus]}</Heading>
          </>
        )}

        <VStack gap="2">
          <div>
            <SakTypeTag sakType={klage.sak.sakType} />
          </div>

          <HStack gap="4">
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
          </HStack>
        </VStack>
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
        apiResult: oppgaveResult,
        errorMessage: 'Kunne ikke hente saksbehandler gjeldende oppgave. Husk å tildele oppgaven.',
      })}
      <DokumentlisteLiten fnr={klage.sak.ident} />
      <AvsluttKlage />
    </Sidebar>
  )
}
