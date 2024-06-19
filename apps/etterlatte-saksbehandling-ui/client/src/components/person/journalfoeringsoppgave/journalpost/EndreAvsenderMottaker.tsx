import { AvsenderMottaker, SoekPersonValg } from '~shared/types/Journalpost'
import { Alert, BodyShort, Button, Heading, HStack, TextField } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import React, { useState } from 'react'
import { InputFlexRow } from './OppdaterJournalpost'
import { fnrHarGyldigFormat } from '~utils/fnr'
import { useForm } from 'react-hook-form'
import SoekPersonPdl from '~components/person/journalfoeringsoppgave/journalpost/modal/SoekPersonPdl'

export const EndreAvsenderMottaker = ({
  avsenderMottaker,
  oppdaterAvsenderMottaker,
}: {
  avsenderMottaker: AvsenderMottaker
  oppdaterAvsenderMottaker: (avsenderMottaker: SoekPersonValg) => void
}) => {
  const {
    register,
    formState: { errors },
    handleSubmit,
    reset,
  } = useForm<SoekPersonValg>({ defaultValues: avsenderMottaker })

  const [rediger, setRediger] = useState(false)
  const [open, setOpen] = useState(false)

  const lagreEndretMottaker = (avsenderMottaker: SoekPersonValg) => {
    oppdaterAvsenderMottaker(avsenderMottaker)
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
                value: /^\d{11}$/,
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
          <SoekPersonPdl open={open} setOpen={setOpen} velgPerson={lagreEndretMottaker} />

          <HStack gap="4" justify="end">
            <Button variant="tertiary" onClick={avbryt} size="small">
              Avbryt
            </Button>
            <Button
              variant="secondary"
              onClick={handleSubmit((avsendermottaker) => lagreEndretMottaker(avsendermottaker))}
              size="small"
            >
              Lagre
            </Button>
            <Button onClick={() => setOpen(!open)}>Avansert Søk</Button>
          </HStack>
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
