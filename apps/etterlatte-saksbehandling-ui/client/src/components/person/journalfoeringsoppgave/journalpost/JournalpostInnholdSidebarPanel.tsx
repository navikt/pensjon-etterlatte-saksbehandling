import { Heading, Tag } from '@navikt/ds-react'
import { Border, InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import React from 'react'
import { Journalpost } from '~shared/types/Journalpost'
import { formaterJournalpostSakstype, formaterJournalpostStatus, formaterStringDato } from '~utils/formattering'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { FlexRow } from '~shared/styled'
import { SidebarPanel } from '~shared/components/Sidebar'
import { temaTilhoererGjenny } from '~components/person/journalfoeringsoppgave/journalpost/validering'

const TemaTag = ({ journalpost }: { journalpost: Journalpost }) => {
  if (temaTilhoererGjenny(journalpost)) return <Tag variant="success">{journalpost.tema}</Tag>
  else return <Tag variant="error">{journalpost.tema}</Tag>
}

export const JournalpostInnholdSidebarPanel = ({ journalpost }: { journalpost: Journalpost }) => (
  <SidebarPanel border>
    <Heading size="small" spacing>
      Journalpostdetailjer
    </Heading>

    <InfoWrapper>
      <Info label="Tema" tekst={<TemaTag journalpost={journalpost} />} />
      <Info label="Kanal/kilde" tekst={journalpost.kanal} />
      <Info label="Status" tekst={formaterJournalpostStatus(journalpost.journalstatus)} />
      <Info
        label="Registrert dato"
        tekst={journalpost.datoOpprettet ? formaterStringDato(journalpost.datoOpprettet) : 'Mangler opprettelsesdato'}
      />
    </InfoWrapper>

    <br />
    <Border />

    <InfoWrapper>
      <Info label="Bruker" tekst={<KopierbarVerdi value={journalpost.bruker.id!!} />} />

      <Info
        label="Avsender/mottaker"
        tekst={
          <FlexRow align="center">
            <span>{journalpost.avsenderMottaker.navn || '-'}</span>
            {journalpost.avsenderMottaker.id && <KopierbarVerdi value={journalpost.avsenderMottaker?.id} />}
          </FlexRow>
        }
      />
    </InfoWrapper>

    <br />
    <Border />

    <InfoWrapper>
      <Info label="SakID" tekst={journalpost.sak?.fagsakId || '-'} />
      <Info
        label="Sakstype"
        tekst={journalpost.sak?.sakstype ? formaterJournalpostSakstype(journalpost.sak?.sakstype) : '-'}
      />
      <Info label="Fagsystem" tekst={journalpost.sak?.fagsaksystem || '-'} />
    </InfoWrapper>
  </SidebarPanel>
)
