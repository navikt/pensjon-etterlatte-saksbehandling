import { AvsenderMottaker } from '~shared/types/Journalpost'
import { Alert, BodyShort, Button, Heading, TextField } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import React, { useState } from 'react'
import { InputFlexRow } from './OppdaterJournalpost'
import { FlexRow } from '~shared/styled'
import { fnrHarGyldigFormat } from '~utils/fnr'
import { useForm } from 'react-hook-form'

export const EndreAvsenderMottaker = ({
  avsenderMottaker,
  oppdaterAvsenderMottaker,
}: {
  avsenderMottaker: AvsenderMottaker
  oppdaterAvsenderMottaker: (avsenderMottaker: AvsenderMottaker) => void
}) => {
  const {
    register,
    formState: { errors },
    handleSubmit,
    getValues,
    reset,
  } = useForm<AvsenderMottaker>({ defaultValues: avsenderMottaker })

  const [rediger, setRediger] = useState(false)

  const lagreEndretMottaker = () => {
    oppdaterAvsenderMottaker(getValues())
    setRediger(false)
  }

  const avbryt = () => {
    reset({ ...avsenderMottaker })
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
            {...register('id', {
              pattern: {
                value: /[0-9]{11}/,
                message: 'Fødselsnummer må bestå av 11 siffer',
              },
            })}
            label="Fødselsnummer"
            error={errors?.id?.message}
          />
          <br />
          <TextField
            {...register('navn', {
              required: {
                value: true,
                message: 'Navn må fylles ut',
              },
            })}
            label="Navn"
            error={errors?.navn?.message}
          />

          <br />

          <FlexRow justify="right">
            <Button variant="tertiary" onClick={avbryt} size="small">
              Avbryt
            </Button>
            <Button variant="secondary" onClick={handleSubmit(lagreEndretMottaker)} size="small">
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
