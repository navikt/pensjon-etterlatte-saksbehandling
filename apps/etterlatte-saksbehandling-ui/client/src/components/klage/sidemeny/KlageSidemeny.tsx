import { useKlage } from '~components/klage/useKlage'
import { Heading, Tag } from '@navikt/ds-react'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { Dokumentoversikt } from '~components/person/dokumenter/dokumentoversikt'
import { teksterKabalstatus, teksterKlagestatus } from '~shared/types/Klage'
import { tagColors, TagList } from '~shared/Tags'
import { formaterSakstype, formaterStringDato } from '~utils/formattering'
import { Info, Tekst } from '~components/behandling/attestering/styled'
import AvsluttKlage from '~components/klage/AvsluttKlage'

export function KlageSidemeny() {
  const klage = useKlage()

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
