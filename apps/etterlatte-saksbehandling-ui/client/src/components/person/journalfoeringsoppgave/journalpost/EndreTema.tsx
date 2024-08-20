import { Journalpost } from '~shared/types/Journalpost'
import { hentKodeverkArkivtemaer, Beskrivelse } from '~shared/api/kodeverk'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isInitial, mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { Alert, UNSAFE_Combobox } from '@navikt/ds-react'
import { temaTilhoererGjenny } from '~components/person/journalfoeringsoppgave/journalpost/validering'

export const EndreTema = ({
  journalpost,
  oppdater,
}: {
  journalpost: Journalpost
  oppdater: (kode: Beskrivelse) => void
}) => {
  const [gammelKode, settGammelKode] = useState<Beskrivelse>()
  const [valgtKode, settValgtKode] = useState<Beskrivelse>()
  const [temaStatus, apiHentTema] = useApiCall(hentKodeverkArkivtemaer)

  useEffect(() => {
    if (isInitial(temaStatus) && !!journalpost) {
      apiHentTema(undefined, (koder: Beskrivelse[]) => {
        const kode = koder.find((kode) => kode.navn === journalpost?.tema)
        settGammelKode(kode)
        settValgtKode(kode)
      })
    }
  }, [journalpost])

  return mapApiResult(
    temaStatus,
    <Spinner label="Henter tilgjengelige temakoder ..." />,
    () => <ApiErrorAlert>Feil ved henting av temakoder</ApiErrorAlert>,
    (koder) => (
      <FormWrapper $column={true}>
        <UNSAFE_Combobox
          label="Tema"
          options={koder.sort((a, b) => a.term.localeCompare(b.term)).map((kode) => kode.term)}
          shouldAutocomplete={true}
          defaultValue={valgtKode?.term}
          onToggleSelected={(option, isSelected) => {
            if (isSelected) {
              const kode = koder.find((kode) => kode.term === option)
              if (kode) {
                settValgtKode(kode)
                oppdater(kode)
              }
            }
          }}
        />

        {!!valgtKode &&
          valgtKode !== gammelKode &&
          (temaTilhoererGjenny(journalpost) ? (
            <Alert variant="info">
              Nytt tema blir <strong>{valgtKode.term}</strong> (temakode <strong>{valgtKode.navn}</strong>)
            </Alert>
          ) : (
            <Alert variant="warning">
              OBS! Du har valgt tema <strong>{valgtKode.term}</strong> (temakode <strong>{valgtKode.navn}</strong>).
              Dette temaet kan ikke behandles i Gjenny. Ved å trykke på «Lagre utkast» vil journalposten blir flyttet
              til enheten som eier tema, og oppgaven i Gjenny blir avsluttet.
            </Alert>
          ))}
      </FormWrapper>
    )
  )
}
