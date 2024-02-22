import { useKlage } from '~components/klage/useKlage'
import { BodyShort, Heading, Tag } from '@navikt/ds-react'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { Dokumentoversikt } from '~components/person/dokumenter/Dokumentoversikt'
import { teksterKabalstatus, teksterKlagestatus } from '~shared/types/Klage'
import { tagColors, TagList } from '~shared/Tags'
import { formaterSakstype, formaterStringDato } from '~utils/formattering'
import { Info, Tekst } from '~components/behandling/attestering/styled'
import AvsluttKlage from '~components/klage/AvsluttKlage'
import React, { useEffect } from 'react'
import { updateVedtakSammendrag } from '~store/reducers/VedtakReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { useAppDispatch } from '~store/Store'
import { mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export function KlageSidemeny() {
  const klage = useKlage()
  const dispatch = useAppDispatch()
  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)

  useEffect(() => {
    if (!klage?.id) return
    fetchVedtakSammendrag(klage.id, (vedtakSammendrag, statusCode) => {
      if (statusCode === 200) {
        dispatch(updateVedtakSammendrag(vedtakSammendrag))
      }
    })
  }, [klage?.id])

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
      <Dokumentoversikt fnr={klage.sak.ident} liten />
      <AvsluttKlage />
    </Sidebar>
  )
}
