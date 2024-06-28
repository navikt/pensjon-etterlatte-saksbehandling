import { Box, Heading, HStack, Tag, VStack } from '@navikt/ds-react'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import React from 'react'
import { Journalpost } from '~shared/types/Journalpost'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { temaTilhoererGjenny } from '~components/person/journalfoeringsoppgave/journalpost/validering'
import { formaterDato } from '~utils/formatering/dato'
import { formaterJournalpostSakstype, formaterJournalpostStatus } from '~utils/formatering/formatering'

const TemaTag = ({ journalpost }: { journalpost: Journalpost }) => {
  if (temaTilhoererGjenny(journalpost)) return <Tag variant="success">{journalpost.tema}</Tag>
  else return <Tag variant="error">{journalpost.tema}</Tag>
}

export const JournalpostInnhold = ({ journalpost }: { journalpost: Journalpost }) => (
  <>
    <Heading size="small" spacing>
      Journalpostdetaljer
    </Heading>

    <VStack gap="4">
      <Info label="Tema" tekst={<TemaTag journalpost={journalpost} />} />
      <Info label="Kanal/kilde" tekst={journalpost.kanal} />
      <Info label="Status" tekst={formaterJournalpostStatus(journalpost.journalstatus)} />
      <Info
        label="Registrert dato"
        tekst={journalpost.datoOpprettet ? formaterDato(journalpost.datoOpprettet) : 'Mangler opprettelsesdato'}
      />
    </VStack>

    <br />
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
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
    </Box>

    <br />
    <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
      <VStack gap="4">
        <Info label="SakID" tekst={journalpost.sak?.fagsakId || '-'} />
        <Info
          label="Sakstype"
          tekst={journalpost.sak?.sakstype ? formaterJournalpostSakstype(journalpost.sak?.sakstype) : '-'}
        />
        <Info label="Fagsystem" tekst={journalpost.sak?.fagsaksystem || '-'} />
      </VStack>
    </Box>
  </>
)
