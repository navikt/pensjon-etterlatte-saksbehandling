import { BodyShort, Box, Button, Heading, HStack, TextField, VStack } from '@navikt/ds-react'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import { NyBehandlingSkjema } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import React from 'react'
import { useFieldArray, useFormContext } from 'react-hook-form'
import {
  validateFnrObligatorisk,
  validerFnrValgfri,
} from '~components/person/journalfoeringsoppgave/nybehandling/validator'

export default function PersongalleriOmstillingsstoenad() {
  const {
    register,
    formState: { errors },
  } = useFormContext<NyBehandlingSkjema>()

  const avdoedFormArray = useFieldArray<NyBehandlingSkjema>({ name: 'persongalleri.avdoed' })
  const soeskenFormArray = useFieldArray<NyBehandlingSkjema>({ name: 'persongalleri.soesken' })

  return (
    <VStack gap="4">
      <TextField
        {...register('persongalleri.soeker')}
        label="Søker (gjenlevende)"
        description="Fødselsnummeret er automatisk hentet fra oppgaven"
        readOnly
      />

      <TextField
        {...register('persongalleri.innsender', { validate: validerFnrValgfri })}
        label="Innsender"
        description="Oppgi innsenderen sitt fødselsnummer (dersom det er tilgjengelig)"
        error={errors.persongalleri?.innsender?.message}
      />

      {avdoedFormArray.fields.map((field, index) => (
        <TextField
          {...register(`persongalleri.avdoed.${index}.value`, { validate: validerFnrValgfri })}
          key={field.id}
          label="Avdød"
          description="Oppgi fødselsnummer (dersom det er tilgjengelig)"
          error={errors?.persongalleri?.avdoed?.[index]?.value?.message}
        />
      ))}

      <Box padding="4" borderWidth="1" borderRadius="small">
        <Heading size="small" spacing>
          Barn
          <BodyShort textColor="subtle">Legg til barn hvis tilgjengelig</BodyShort>
        </Heading>

        <VStack gap="4">
          {soeskenFormArray.fields.map((field, index) => (
            <HStack key={index} align="end">
              <TextField
                {...register(`persongalleri.soesken.${index}.value`, { validate: validateFnrObligatorisk })}
                key={field.id}
                label="Barn"
                error={errors?.persongalleri?.soesken?.[index]?.value?.message}
              />
              <Button
                icon={<XMarkIcon aria-hidden />}
                variant="tertiary"
                onClick={() => soeskenFormArray.remove(index)}
              />
            </HStack>
          ))}

          <div>
            <Button
              variant="secondary"
              icon={<PlusIcon aria-hidden />}
              onClick={() => soeskenFormArray.append({ value: '' })}
              type="button"
            >
              Legg til barn
            </Button>
          </div>
        </VStack>
      </Box>
    </VStack>
  )
}
