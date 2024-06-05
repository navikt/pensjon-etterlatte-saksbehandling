import React from 'react'
import { FieldErrors, UseFormRegister } from 'react-hook-form'
import { FormdataVurdering } from '~components/klage/vurdering/EndeligVurdering'
import { Heading, Select, Textarea } from '@navikt/ds-react'
import { SmalVStack } from '~components/klage/styled'
import { AARSAKER_OMGJOERING, TEKSTER_AARSAK_OMGJOERING } from '~shared/types/Klage'

export const KlageOmgjoering = ({
  register,
  errors,
}: {
  register: UseFormRegister<FormdataVurdering>
  errors: FieldErrors<FormdataVurdering>
}) => {
  return (
    <SmalVStack gap="4">
      <Heading level="3" size="medium">
        Omgjøring
      </Heading>

      <Select
        label="Hvorfor skal saken omgjøres?"
        error={errors.omgjoering?.grunnForOmgjoering?.message}
        {...register('omgjoering.grunnForOmgjoering', {
          required: {
            value: true,
            message: 'Du må velge en årsak for omgjøringen.',
          },
        })}
      >
        <option value="">Velg grunn</option>
        {AARSAKER_OMGJOERING.map((aarsak) => (
          <option key={aarsak} value={aarsak}>
            {TEKSTER_AARSAK_OMGJOERING[aarsak]}
          </option>
        ))}
      </Select>

      <Textarea
        label="Begrunnelse"
        error={errors.omgjoering?.begrunnelse?.message}
        {...register('omgjoering.begrunnelse', {
          required: {
            value: true,
            message: 'Du må gi en begrunnelse for omgjøringen.',
          },
        })}
      />
    </SmalVStack>
  )
}
