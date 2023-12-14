import { Heading } from '@navikt/ds-react'
import { Border, InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import React from 'react'
import { Journalpost } from '~shared/types/Journalpost'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { formaterStringDato } from '~utils/formattering'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { FlexRow } from '~shared/styled'

export const InnholdJournalpost = ({ journalpost }: { journalpost: Journalpost }) => {
  return (
    <>
      <Heading size="medium" spacing>
        Innhold
      </Heading>
      <InfoWrapper>
        <Info label="Kanal/kilde" tekst={journalpost.kanal} />
        <Info
          label="Registrert dato"
          tekst={journalpost.datoOpprettet ? formaterStringDato(journalpost.datoOpprettet) : 'Mangler opprettelsesdato'}
        />
        <Info label="Status" tekst={journalpost.journalstatus} />
      </InfoWrapper>

      <br />

      <Border />

      <Heading size="small" spacing>
        Gjelder
      </Heading>

      <FormWrapper column={true}>
        <InfoWrapper>
          <Info label="Tema" tekst={journalpost.tema} />

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

        <Border />

        <Heading size="small">Sak</Heading>

        <InfoWrapper>
          <Info label="SakID" tekst={journalpost.sak?.fagsakId || '-'} />
          <Info label="Sakstype" tekst={journalpost.sak?.sakstype || '-'} />
          <Info label="Fagsystem" tekst={journalpost.sak?.fagsaksystem || '-'} />
          <Info label="Tema" tekst={journalpost.sak?.tema || '-'} />
        </InfoWrapper>
      </FormWrapper>
    </>
  )
}
