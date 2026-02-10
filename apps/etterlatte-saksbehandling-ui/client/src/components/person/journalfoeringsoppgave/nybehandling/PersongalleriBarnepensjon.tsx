import React from 'react'
import { BodyShort, Box, Button, Heading, HStack, TextField, VStack } from '@navikt/ds-react'
import { NyBehandlingSkjema } from './OpprettNyBehandling'
import { useFieldArray, useFormContext } from 'react-hook-form'
import {
  validateFnrObligatorisk,
  validerFnrValgfri,
} from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'

export default function PersongalleriBarnepensjon({ erManuellMigrering = false }: { erManuellMigrering?: boolean }) {
  const {
    register,
    formState: { errors },
  } = useFormContext<NyBehandlingSkjema>()

  const gjenlevendeFormArray = useFieldArray<NyBehandlingSkjema>({ name: 'persongalleri.gjenlevende' })
  const avdoedFormArray = useFieldArray<NyBehandlingSkjema>({ name: 'persongalleri.avdoed' })
  const soeskenFormArray = useFieldArray<NyBehandlingSkjema>({ name: 'persongalleri.soesken' })

  const kanLeggeTil: boolean = (gjenlevendeFormArray.fields.length || 0) + (avdoedFormArray.fields.length || 0) < 2

  return (
    <VStack gap="space-4">
      <TextField
        {...register('persongalleri.soeker', { validate: validateFnrObligatorisk })}
        label="Søker (barnet)"
        description={
          erManuellMigrering ? 'Oppgi søker sitt fødselsnummer' : 'Fødselsnummeret er automatisk hentet fra oppgaven'
        }
        error={errors.persongalleri?.soeker?.message}
        readOnly={!erManuellMigrering}
      />

      <Box width="20rem">
        <TextField
          {...register('persongalleri.innsender', { validate: validerFnrValgfri })}
          label="Innsender"
          description="Oppgi innsenderen sitt fødselsnummer (dersom det er tilgjengelig)"
          error={errors.persongalleri?.innsender?.message}
        />
      </Box>

      <Box padding="space-4" borderWidth="1">
        <Heading size="small" spacing>
          Biologisk forelder
          <BodyShort textColor="subtle">Legg til biologisk forelder hvis tilgjengelig</BodyShort>
        </Heading>

        <VStack gap="space-4" align="start">
          {gjenlevendeFormArray.fields.map((field, index) => (
            <HStack gap="space-2" key={index} align="end">
              <Box width="20rem">
                <TextField
                  {...register(`persongalleri.gjenlevende.${index}.value`, { validate: validateFnrObligatorisk })}
                  key={field.id}
                  label="Biologisk forelder"
                  description="Oppgi fødselsnummer"
                  error={errors?.persongalleri?.gjenlevende?.[index]?.value?.message}
                />
              </Box>
              <div>
                <Button
                  icon={<XMarkIcon aria-hidden />}
                  variant="tertiary"
                  onClick={() => gjenlevendeFormArray.remove(index)}
                />
              </div>
            </HStack>
          ))}
          <Button
            icon={<PlusIcon aria-hidden />}
            onClick={() => gjenlevendeFormArray.append({ value: '' })}
            disabled={!kanLeggeTil}
            type="button"
          >
            Legg til biologisk forelder
          </Button>
        </VStack>
      </Box>

      <Box padding="space-4" borderWidth="1">
        <Heading size="small" spacing>
          Avdød forelder
          <BodyShort textColor="subtle">Legg til avdød hvis tilgjengelig</BodyShort>
        </Heading>

        <VStack gap="space-4" align="start">
          {avdoedFormArray.fields.map((field, index) => (
            <HStack gap="space-2" key={index} align="end">
              <Box width="20rem">
                <TextField
                  {...register(`persongalleri.avdoed.${index}.value`, { validate: validateFnrObligatorisk })}
                  key={field.id}
                  label="Avdød forelder"
                  description="Oppgi fødselsnummer"
                  error={errors?.persongalleri?.avdoed?.[index]?.value?.message}
                />
              </Box>
              <div>
                <Button
                  icon={<XMarkIcon aria-hidden />}
                  variant="tertiary"
                  onClick={() => avdoedFormArray.remove(index)}
                />
              </div>
            </HStack>
          ))}
          <Button
            icon={<PlusIcon aria-hidden />}
            onClick={() => avdoedFormArray.append({ value: '' })}
            disabled={!kanLeggeTil}
            type="button"
          >
            Legg til avdød
          </Button>
        </VStack>
      </Box>

      <Box padding="space-4" borderWidth="1">
        <Heading size="small" spacing>
          Søsken
          <BodyShort textColor="subtle">Legg til barn hvis tilgjengelig</BodyShort>
        </Heading>
        <VStack gap="space-4" align="start">
          {soeskenFormArray.fields.map((field, index) => (
            <HStack gap="space-2" key={index} align="end">
              <Box width="20rem">
                <TextField
                  {...register(`persongalleri.soesken.${index}.value`, { validate: validateFnrObligatorisk })}
                  key={field.id}
                  label="Søsken"
                  description="Oppgi fødselsnummer"
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
            Legg til søsken
          </Button>
        </VStack>
      </Box>
    </VStack>
  )
}
