import React from 'react'
import { FieldErrors, UseFormRegister } from 'react-hook-form'
import { FormdataVurdering } from '~components/klage/vurdering/EndeligVurdering'
import { useKlage } from '~components/klage/useKlage'
import { SakType } from '~shared/types/sak'
import { LOVHJEMLER_BP, LOVHJEMLER_OMS, TEKSTER_LOVHJEMLER } from '~shared/types/Klage'
import { Box, ErrorMessage, Heading, Select, Textarea, VStack } from '@navikt/ds-react'

export const KlageInnstilling = ({
  register,
  errors,
}: {
  register: UseFormRegister<FormdataVurdering>
  errors: FieldErrors<FormdataVurdering>
}) => {
  const klage = useKlage()
  const aktuelleHjemler = klage?.sak.sakType === SakType.BARNEPENSJON ? LOVHJEMLER_BP : LOVHJEMLER_OMS

  return (
    <VStack gap="space-4" width="41.5rem">
      <Heading level="3" size="medium">
        Innstilling til KA
      </Heading>

      <Box width="fit-content">
        <Select
          {...register('innstilling.lovhjemmel', {
            required: true,
          })}
          label="Hjemmel"
          description="Velg hvilken hjemmel klagen knytter seg til"
        >
          <option value="">Velg hjemmel</option>
          {aktuelleHjemler.map((hjemmel) => (
            <option key={hjemmel} value={hjemmel}>
              {TEKSTER_LOVHJEMLER[hjemmel]}
            </option>
          ))}
        </Select>
        {errors.innstilling?.lovhjemmel && (
          <ErrorMessage>Du må angi hjemmelen klagen hovedsakelig knytter seg til.</ErrorMessage>
        )}
      </Box>

      <Textarea
        {...register('innstilling.innstillingTekst', {
          required: {
            value: true,
            message: 'Du må skrive en innstillingstekst som begrunner hvorfor klagen står seg.',
          },
        })}
        label="Innstilling"
        description="Innstillingen blir med i brev til klager og til KA"
        error={errors.innstilling?.innstillingTekst?.message}
      />

      <Textarea
        {...register('innstilling.internKommentar')}
        label="Intern kommentar til KA (valgfri)"
        description="Kommentaren blir ikke synlig for bruker"
      />
    </VStack>
  )
}
