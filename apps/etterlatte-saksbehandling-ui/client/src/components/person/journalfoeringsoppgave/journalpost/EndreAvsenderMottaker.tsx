import { AvsenderMottaker } from '~shared/types/Journalpost'
import { Alert, BodyShort, Button, Heading, TextField } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import React, { useState } from 'react'
import { InputFlexRow } from './OppdaterJournalpost'
import { FlexRow } from '~shared/styled'
import { fnrHarGyldigFormat } from '~utils/fnr'

export const EndreAvsenderMottaker = ({
  initiellAvsenderMottaker,
  oppdaterAvsenderMottaker,
}: {
  initiellAvsenderMottaker: AvsenderMottaker
  oppdaterAvsenderMottaker: (avsenderMottaker: AvsenderMottaker) => void
}) => {
  const [avsenderMottaker, setAvsenderMottaker] = useState(initiellAvsenderMottaker)
  const [rediger, setRediger] = useState(false)

  const lagreEndretMottaker = () => {
    oppdaterAvsenderMottaker(avsenderMottaker)
    setRediger(false)
  }

  const avbryt = () => {
    setAvsenderMottaker(initiellAvsenderMottaker)
    setRediger(false)
  }

  return (
    <div>
      <Heading size="small" spacing>
        Avsender/mottaker
      </Heading>

      {rediger ? (
        <>
          <TextField
            label="Fødselsnummer"
            value={avsenderMottaker.id || ''}
            onChange={(e) => setAvsenderMottaker({ ...avsenderMottaker, id: e.target.value })}
          />
          <br />
          <TextField
            label="Navn"
            value={avsenderMottaker.navn || ''}
            onChange={(e) => setAvsenderMottaker({ ...avsenderMottaker, navn: e.target.value })}
          />

          <br />

          <FlexRow justify="right">
            <Button variant="tertiary" onClick={avbryt} size="small">
              Avbryt
            </Button>
            <Button variant="secondary" onClick={lagreEndretMottaker} size="small">
              Lagre
            </Button>
          </FlexRow>
        </>
      ) : (
        <>
          <InputFlexRow>
            <BodyShort spacing className="flex" as="div">
              {avsenderMottaker.navn || '-'}
              {avsenderMottaker.id && <KopierbarVerdi value={avsenderMottaker?.id} />}
            </BodyShort>

            <Button variant="secondary" size="small" onClick={() => setRediger(true)}>
              Endre
            </Button>
          </InputFlexRow>

          <br />

          {!avsenderMottaker.navn && !fnrHarGyldigFormat(avsenderMottaker.id) && (
            <Alert variant="warning">Avsender/mottaker må ha et gyldig fnr. hvis navn ikke er satt</Alert>
          )}
        </>
      )}
    </div>
  )
}
