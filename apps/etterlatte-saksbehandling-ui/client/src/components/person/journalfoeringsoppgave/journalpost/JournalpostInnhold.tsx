import { Heading, HStack, Tag, VStack } from '@navikt/ds-react'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import React from 'react'
import { Journalpost } from '~shared/types/Journalpost'
import { formaterJournalpostSakstype, formaterJournalpostStatus, formaterStringDato } from '~utils/formattering'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { temaTilhoererGjenny } from '~components/person/journalfoeringsoppgave/journalpost/validering'

const TemaTag = ({ journalpost }: { journalpost: Journalpost }) => {
  if (temaTilhoererGjenny(journalpost)) return <Tag variant="success">{journalpost.tema}</Tag>
  else return <Tag variant="error">{journalpost.tema}</Tag>
}

export const JournalpostInnhold = ({ journalpost }: { journalpost: Journalpost }) => (
  <>
    <Heading size="small" spacing>
      Journalpostdetailjer
    </Heading>

    <VStack gap="4">
      <Info label="Tema" tekst={<TemaTag journalpost={journalpost} />} />
      <Info label="Kanal/kilde" tekst={journalpost.kanal} />
      <Info label="Status" tekst={formaterJournalpostStatus(journalpost.journalstatus)} />
      <Info
        label="Registrert dato"
        tekst={journalpost.datoOpprettet ? formaterStringDato(journalpost.datoOpprettet) : 'Mangler opprettelsesdato'}
      />
    </VStack>

    <br />
    <Border />

    <VStack gap="4">
      <Info label="Bruker" tekst={journalpost.bruker?.id ? <KopierbarVerdi value={journalpost.bruker.id} /> : '-'} />

      <Info
        label="Avsender/mottaker"
        tekst={
          <HStack gap="4" align="center">
            <span>{journalpost.avsenderMottaker.navn || '-'}</span>
            {journalpost.avsenderMottaker.id && <KopierbarVerdi value={journalpost.avsenderMottaker?.id} />}
          </HStack>
        }
      />
    </VStack>

    <br />
    <Border />

    <VStack gap="4">
      <Info label="SakID" tekst={journalpost.sak?.fagsakId || '-'} />
      <Info
        label="Sakstype"
        tekst={journalpost.sak?.sakstype ? formaterJournalpostSakstype(journalpost.sak?.sakstype) : '-'}
      />
      <Info label="Fagsystem" tekst={journalpost.sak?.fagsaksystem || '-'} />
    </VStack>
  </>
)
