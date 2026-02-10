import { AvsenderMottaker, AvsenderMottakerIdType } from '~shared/types/Journalpost'
import { Alert, BodyShort, Box, Button, Heading, HStack, Label, Select, TextField, VStack } from '@navikt/ds-react'
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
    watch,
  } = useForm<AvsenderMottaker>({ defaultValues: avsenderMottaker })

  const [rediger, setRediger] = useState(false)

  const lagreEndring = (avsenderMottaker: AvsenderMottaker) => {
    oppdaterAvsenderMottaker({
      ...avsenderMottaker,
      idType: fnrHarGyldigFormat(avsenderMottaker.id) ? AvsenderMottakerIdType.FNR : avsenderMottaker.idType,
    })
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
        <Box padding="space-4" borderWidth="1" borderColor="neutral-subtle">
          <VStack gap="space-8">
            <VStack gap="space-4">
              <TextField
                {...register('id')}
                label="ID"
                description="Må være et fødselsnummer, organisasjonsnummer, HPRNR (Helsepersonellregisterets identifikator), eller en utenlandsk organisasjon"
                error={errors?.id?.message}
                htmlSize={40}
              />

              {watch('idType') === AvsenderMottakerIdType.FNR && !fnrHarGyldigFormat(watch('id')) && (
                <Alert variant="warning">
                  ID-type er fødselsnummer, men ID-en ser ikke ut til å være et gyldig fødselsnummer. Er du sikker på at
                  det er korrekt?
                </Alert>
              )}

              <Select
                {...register('idType', { required: { value: !!watch('id')?.length, message: 'Påkrevd' } })}
                label="ID type"
                error={errors?.idType?.message}
              >
                <option value="">Velg type ...</option>
                <option value={AvsenderMottakerIdType.FNR}>Fødselsnummer</option>
                <option value={AvsenderMottakerIdType.ORGNR}>Organisasjonsnummer</option>
                <option value={AvsenderMottakerIdType.AKTOERID}>AktørID</option>
                <option value={AvsenderMottakerIdType.UTL_ORG}>Utenlandsk organisasjon</option>
                <option value={AvsenderMottakerIdType.HPRNR}>Helsepersonellregisterets identifikator</option>
              </Select>

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

            <VStack gap="space-4">
              <Label>Har du ikke fødselsnummeret?</Label>
              <PersonSoekModal
                velgPerson={({ id, navn }) => {
                  setValue('id', id, { shouldDirty: true, shouldValidate: true })
                  setValue('idType', AvsenderMottakerIdType.FNR)
                  setValue('navn', navn, { shouldDirty: true, shouldValidate: true })
                }}
              />
            </VStack>
          </VStack>

          <br />

          <HStack gap="space-4" justify="end">
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
