import { AvsenderMottaker, BrukerIdType } from '~shared/types/Journalpost'
import { Alert, BodyShort, Box, Button, Heading, HStack, Label, TextField, VStack } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import React, { useState } from 'react'
import { InputFlexRow } from './OppdaterJournalpost'
import { fnrHarGyldigFormat } from '~utils/fnr'
import { useForm } from 'react-hook-form'
import { PersonSoekModal } from '~components/person/journalfoeringsoppgave/journalpost/modal/PersonSoekModal'

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
    reset,
    setValue,
  } = useForm<AvsenderMottaker>({ defaultValues: avsenderMottaker })

  const [rediger, setRediger] = useState(false)

  const lagreEndring = (avsenderMottaker: AvsenderMottaker) => {
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
        <Box background="bg-subtle" padding="4" borderColor="border-subtle" borderWidth="1" borderRadius="medium">
          <VStack gap="8">
            <VStack gap="4">
              <TextField
                {...register('id', {
                  pattern: {
                    value: /^\d{11}$/,
                    message: 'Fødselsnummer må bestå av 11 siffer',
                  },
                })}
                label="Fødselsnummer"
                error={errors?.id?.message}
                htmlSize={40}
              />
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
            </VStack>

            <VStack gap="4">
              <Label>Har du ikke fødselsnummeret?</Label>
              <PersonSoekModal
                velgPerson={({ id, navn }) => {
                  setValue('id', id, { shouldDirty: true, shouldValidate: true })
                  setValue('idType', BrukerIdType.FNR)
                  setValue('navn', navn, { shouldDirty: true, shouldValidate: true })
                }}
              />
            </VStack>
          </VStack>

          <br />

          <HStack gap="4" justify="end">
            <Button variant="tertiary" onClick={avbryt} size="small">
              Avbryt
            </Button>
            <Button onClick={handleSubmit((avsendermottaker) => lagreEndring(avsendermottaker))} size="small">
              Lagre
            </Button>
          </HStack>
        </Box>
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
