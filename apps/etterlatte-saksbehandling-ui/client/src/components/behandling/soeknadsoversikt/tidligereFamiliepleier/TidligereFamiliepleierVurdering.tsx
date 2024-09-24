import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { Box, Button, Heading, HStack, Radio, Textarea, TextField, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { JaNei } from '~shared/types/ISvar'
import TidligereFamiliepleierVisning from '~components/behandling/soeknadsoversikt/tidligereFamiliepleier/TidligereFamiliepleierVisning'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { fnrErGyldig } from '~utils/fnr'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'

export interface TidligereFamiliepleierValues {
  svar: JaNei | null
  foedselsnummer?: string | null
  opphoertPleieforhold?: Date | null
  begrunnelse: string
}

export const TidligereFamiliepleierValuesDefault: TidligereFamiliepleierValues = {
  svar: null,
  foedselsnummer: null,
  opphoertPleieforhold: undefined,
  begrunnelse: '',
}

export const TidligereFamiliepleierVurdering = ({
  redigerbar,
  setVurdert,
  behandlingId,
}: {
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  behandlingId: string
}) => {
  console.log(behandlingId)

  const [rediger, setRediger] = useState<boolean>(redigerbar ?? false)

  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
    watch,
    reset,
  } = useForm<TidligereFamiliepleierValues>({
    defaultValues: TidligereFamiliepleierValuesDefault,
  })

  const lagre = (data: TidligereFamiliepleierValues) => {
    console.log(data)
    setVurdert(true)
    setRediger(false)
  }

  const avbryt = () => {
    reset(TidligereFamiliepleierValuesDefault)
    setRediger(false)
  }

  const svar = watch('svar')

  return (
    <VurderingsboksWrapper
      tittel="Er gjenlevende tidligere familiepleier?"
      subtittelKomponent={<TidligereFamiliepleierVisning tidligereFamiliepleier={null} />}
      redigerbar={redigerbar}
      vurdering={{
        saksbehandler: '123456',
        tidspunkt: new Date(),
      }}
      kommentar={undefined}
      defaultRediger={rediger}
      overstyrRediger={rediger}
      setOverstyrRediger={setRediger}
    >
      <>
        <Heading level="3" size="small">
          Er gjenlevende tidligere familiepleier?
        </Heading>

        <Box width="15rem">
          <VStack gap="4">
            <ControlledRadioGruppe
              name="svar"
              legend=""
              size="small"
              control={control}
              errorVedTomInput="Du må velge om gjenlevende er tidligere familiepleier"
              radios={
                <HStack gap="4">
                  <Radio size="small" value={JaNei.JA}>
                    Ja
                  </Radio>
                  <Radio size="small" value={JaNei.NEI}>
                    Nei
                  </Radio>
                </HStack>
              }
            />
            {svar === JaNei.JA && (
              <>
                <TextField
                  label="Fødselsnummer for forpleiede"
                  autoComplete="off"
                  {...register('foedselsnummer', {
                    required: { value: true, message: 'Du må fylle inn fødselsnummer' },
                    validate: {
                      fnrErGyldig: (value) => fnrErGyldig(value!!) || 'Ugyldig fødselsnummer',
                    },
                  })}
                  error={errors.foedselsnummer?.message}
                />
                <ControlledDatoVelger
                  name="opphoertPleieforhold"
                  label="Pleieforholdet opphørte"
                  control={control}
                  toDate={new Date()}
                />
              </>
            )}
            <Textarea
              label="Begrunnelse"
              minRows={3}
              autoComplete="off"
              {...register('begrunnelse', {
                required: { value: true, message: 'Du må skrive en begrunnelse' },
              })}
            />
            <HStack gap="3">
              <Button variant="primary" type="button" size="small" onClick={handleSubmit(lagre)}>
                Lagre
              </Button>
              <Button variant="secondary" type="button" size="small" onClick={avbryt}>
                Avbryt
              </Button>
            </HStack>
          </VStack>
        </Box>
      </>
    </VurderingsboksWrapper>
  )
}
