import { Box, Heading, HStack, Tag, VStack } from '@navikt/ds-react'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import React from 'react'
import { Datotype, Journalpost, Journalposttype, teksterDatotype } from '~shared/types/Journalpost'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { temaTilhoererGjenny } from '~components/person/journalfoeringsoppgave/journalpost/validering'
import { formaterDato } from '~utils/formatering/dato'
import { formaterJournalpostSakstype, formaterJournalpostStatus } from '~utils/formatering/formatering'

const TemaTag = ({ journalpost }: { journalpost: Journalpost }) => {
  if (temaTilhoererGjenny(journalpost))
    return (
      <Tag data-color="success" variant="outline">
        {journalpost.tema}
      </Tag>
    )
  else
    return (
      <Tag data-color="danger" variant="outline">
        {journalpost.tema}
      </Tag>
    )
}

const datotypeLabel = (datotype: Datotype, journalpost: Journalpost) => {
  if (datotype !== Datotype.DATO_JOURNALFOERT) return teksterDatotype[datotype] ?? datotype

  switch (journalpost.journalposttype) {
    case Journalposttype.I:
    case Journalposttype.N:
      return 'Dato journalført'
    case Journalposttype.U:
      return 'Dato ferdigstilt'
  }
}

export const JournalpostInnhold = ({ journalpost }: { journalpost: Journalpost }) => (
  <>
    <Heading size="small" spacing>
      Journalpostdetaljer
    </Heading>

    <VStack gap="space-4">
      <Info label="Tema" tekst={<TemaTag journalpost={journalpost} />} />
      <Info label="Kanal/kilde" tekst={journalpost.kanal} />
      <Info label="Status" tekst={formaterJournalpostStatus(journalpost.journalstatus)} />
      <HStack gap="space-4">
        <Info
          label="Registrert dato"
          tekst={journalpost.datoOpprettet ? formaterDato(journalpost.datoOpprettet) : 'Mangler opprettelsesdato'}
        />
        {[...journalpost.relevanteDatoer]
          .sort((a) => (a.datotype == Datotype.DATO_SENDT_PRINT ? -1 : 1))
          .map((dato) => (
            <Info
              label={datotypeLabel(dato.datotype, journalpost)}
              tekst={formaterDato(dato.dato)}
              key={dato.datotype}
            />
          ))}
      </HStack>
    </VStack>

    <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="neutral-subtle">
      <VStack gap="space-4">
        <Info label="Bruker" tekst={journalpost.bruker?.id ? <KopierbarVerdi value={journalpost.bruker.id} /> : '-'} />

        <Info
          label="Avsender/mottaker"
          tekst={
            <HStack gap="space-4" align="center">
              <span>{journalpost.avsenderMottaker.navn || '-'}</span>
              {journalpost.avsenderMottaker.id && <KopierbarVerdi value={journalpost.avsenderMottaker?.id} />}
            </HStack>
          }
        />
      </VStack>
    </Box>

    <br />
    <Box paddingBlock="space-4 space-0" borderWidth="1 0 0 0" borderColor="neutral-subtle">
      <VStack gap="space-4">
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
