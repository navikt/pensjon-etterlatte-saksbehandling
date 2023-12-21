import { Alert, BodyShort, Button, List, Panel } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { isPending } from '~shared/api/apiUtils'
import React from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { feilregistrerSakstilknytning, opphevFeilregistrertSakstilknytning } from '~shared/api/dokument'
import { Journalpost } from '~shared/types/Journalpost'

export const FeilregistrerJournalpost = ({
  valgtJournalpost,
  oppdaterJournalposter,
}: {
  valgtJournalpost: Journalpost
  oppdaterJournalposter: () => void
}) => {
  const [feilSakstilknytningStatus, apiFeilregistrerSakstilknytning] = useApiCall(feilregistrerSakstilknytning)
  const [opphevFeilregisrteringStatus, apiOpphevFeilregistrering] = useApiCall(opphevFeilregistrertSakstilknytning)

  const opphevFeilregistrering = () => {
    if (!valgtJournalpost) return

    apiOpphevFeilregistrering(valgtJournalpost.journalpostId, () => {
      oppdaterJournalposter()
    })
  }

  const feilregistrer = () => {
    if (!valgtJournalpost) return

    apiFeilregistrerSakstilknytning(valgtJournalpost.journalpostId, () => {
      oppdaterJournalposter()
    })
  }

  return (
    <Panel>
      <BodyShort spacing>
        Du har valgt journalpost med ID {valgtJournalpost.journalpostId}.
        <br />
        Med dokument(er):
      </BodyShort>
      <List>
        {valgtJournalpost.dokumenter.map((dok) => (
          <List.Item key={dok.dokumentInfoId}>{dok.tittel}</List.Item>
        ))}
      </List>

      {valgtJournalpost?.journalstatus === 'FEILREGISTRERT' ? (
        <Alert variant="info">
          Journalposten er markert som feilregistrert
          <br />
          (sakid {valgtJournalpost.sak?.fagsakId ? `er ${valgtJournalpost.sak?.fagsakId}` : 'mangler'} og tilhører
          fagsystem {valgtJournalpost.sak?.fagsaksystem})
        </Alert>
      ) : (
        <Alert variant="warning">
          Journalposten tilhører ikke Gjenny (EY)!
          <br />
          Sakid {valgtJournalpost.sak?.fagsakId ? `er ${valgtJournalpost.sak?.fagsakId}` : 'mangler'} og tilhører
          fagsystem {valgtJournalpost.sak?.fagsaksystem}
        </Alert>
      )}

      <br />

      <FlexRow justify="right">
        {valgtJournalpost?.journalstatus === 'FEILREGISTRERT' ? (
          <Button
            variant="secondary"
            onClick={opphevFeilregistrering}
            loading={isPending(opphevFeilregisrteringStatus)}
          >
            Opphev feilregistrering
          </Button>
        ) : (
          <Button variant="danger" onClick={feilregistrer} loading={isPending(feilSakstilknytningStatus)}>
            Feilregistrer sakstilknytning
          </Button>
        )}
      </FlexRow>
    </Panel>
  )
}
