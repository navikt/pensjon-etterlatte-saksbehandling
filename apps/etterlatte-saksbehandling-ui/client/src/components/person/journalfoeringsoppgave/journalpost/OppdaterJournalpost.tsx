import { Alert, Heading } from '@navikt/ds-react'
import { Border, InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { FlexRow } from '~shared/styled'
import React, { useState } from 'react'
import { Journalpost } from '~shared/types/Journalpost'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { ISak } from '~shared/types/sak'
import { formaterStringDato } from '~utils/formattering'
import styled from 'styled-components'
import { EndreTema } from '~components/person/journalfoeringsoppgave/journalpost/EndreTema'
import { EndreBruker } from '~components/person/journalfoeringsoppgave/journalpost/EndreBruker'
import { EndreAvsenderMottaker } from '~components/person/journalfoeringsoppgave/journalpost/EndreAvsenderMottaker'
import { EndreSak } from '~components/person/journalfoeringsoppgave/journalpost/EndreSak'
import { EndreDokumenter } from '~components/person/journalfoeringsoppgave/journalpost/EndreDokumenter'
import FerdigstillJournalpostModal from '~components/person/journalfoeringsoppgave/journalpost/modal/FerdigstillJournalpostModal'
import LagreJournalpostModal from '~components/person/journalfoeringsoppgave/journalpost/modal/LagreJournalpostModal'
import { EndreTittelJournalpost } from '~components/person/journalfoeringsoppgave/journalpost/EndreTittelJournalpost'

interface Props {
  initialJournalpost: Journalpost
  sak: ISak
  oppgaveId: string
}

export const OppdaterJournalpost = ({ initialJournalpost, oppgaveId, sak }: Props) => {
  const [journalpost, setJournalpost] = useState<Journalpost>({ ...initialJournalpost })

  return (
    <>
      <Heading size="medium" spacing>
        Journalføringsoppgave
      </Heading>

      <Alert variant="info">
        Journalpost må ferdigstilles eller overføres til annet tema før selve oppgaven kan behandles
      </Alert>

      <br />

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

      <Heading size="medium" spacing>
        Gjelder
      </Heading>

      <FormWrapper column={true}>
        <EndreTema journalpost={journalpost} oppdater={(kode) => setJournalpost({ ...journalpost, tema: kode.navn })} />

        <EndreBruker bruker={journalpost.bruker} />

        <EndreTittelJournalpost
          journalpost={journalpost}
          oppdaterTittelJournalpost={(nyTittel) => setJournalpost({ ...journalpost, tittel: nyTittel })}
        />
        <EndreAvsenderMottaker
          avsenderMottaker={journalpost.avsenderMottaker}
          oppdaterAvsenderMottaker={(avsenderMottaker) => setJournalpost({ ...journalpost, avsenderMottaker })}
        />

        <EndreDokumenter
          initielleDokumenter={journalpost.dokumenter}
          oppdater={(dokumenter) => setJournalpost({ ...journalpost, dokumenter })}
        />

        <EndreSak
          fagsak={journalpost.sak}
          gjennySak={sak}
          kobleTilSak={(sak) => setJournalpost({ ...journalpost, sak })}
        />

        <div>
          <FlexRow justify="center" $spacing>
            <LagreJournalpostModal journalpost={journalpost} oppgaveId={oppgaveId} />

            <FerdigstillJournalpostModal journalpost={journalpost} sak={sak} />
          </FlexRow>
          <FlexRow justify="center">
            <AvbrytBehandleJournalfoeringOppgave />
          </FlexRow>
        </div>
      </FormWrapper>
    </>
  )
}

export const InputFlexRow = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 0.5rem;

  > :not(:last-child) {
    flex: 1;
  }
`
