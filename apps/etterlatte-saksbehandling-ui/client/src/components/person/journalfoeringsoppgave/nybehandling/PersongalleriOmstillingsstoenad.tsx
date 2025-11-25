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
      <Box width="20rem">
        <TextField
          {...register('persongalleri.soeker')}
          label="Søker (gjenlevende)"
          description="Fødselsnummeret er automatisk hentet fra oppgaven"
          readOnly
        />
      </Box>
      <Box width="20rem">
        <TextField
          {...register('persongalleri.innsender', { validate: validerFnrValgfri })}
          label="Innsender"
          description="Oppgi innsenderen sitt fødselsnummer (dersom det er tilgjengelig)"
          error={errors.persongalleri?.innsender?.message}
        />
      </Box>
      {avdoedFormArray.fields.map((field, index) => (
        <Box width="20rem" key={index}>
          <TextField
            {...register(`persongalleri.avdoed.${index}.value`, {
              validate: (fnr) => validerFnrValgfri(fnr ?? null),
            })}
            key={field.id}
            label="Avdød"
            description="Oppgi fødselsnummer (dersom det er tilgjengelig)"
            error={errors?.persongalleri?.avdoed?.[index]?.value?.message}
          />
        </Box>
      ))}
      <Box padding="4" borderWidth="1" borderRadius="small">
        <Heading size="small" spacing>
          Barn
          <BodyShort textColor="subtle">Legg til barn hvis tilgjengelig</BodyShort>
        </Heading>

        <VStack gap="4" align="start">
          {soeskenFormArray.fields.map((field, index) => (
            <HStack gap="2" key={index} align="end">
              <Box width="20rem">
                <TextField
                  {...register(`persongalleri.soesken.${index}.value`, { validate: validateFnrObligatorisk })}
                  key={field.id}
                  label="Barn"
                  error={errors?.persongalleri?.soesken?.[index]?.value?.message}
                />
              </Box>
              <div>
                <Button
                  icon={<XMarkIcon aria-hidden />}
                  variant="tertiary"
                  onClick={() => soeskenFormArray.remove(index)}
                />
              </div>
            </HStack>
          ))}
          <Button icon={<PlusIcon aria-hidden />} onClick={() => soeskenFormArray.append({ value: '' })} type="button">
            Legg til barn
          </Button>
        </VStack>
      </Box>
    </VStack>
  )
}
