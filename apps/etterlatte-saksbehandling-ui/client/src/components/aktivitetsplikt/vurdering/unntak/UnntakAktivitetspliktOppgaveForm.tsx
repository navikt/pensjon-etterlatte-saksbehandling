import {
  AktivitetspliktUnntakType,
  IAktivitetspliktUnntak,
  IOpprettAktivitetspliktUnntak,
  tekstAktivitetspliktUnntakType,
} from '~shared/types/Aktivitetsplikt'
import { useForm } from 'react-hook-form'
import { Button, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { FloppydiskIcon } from '@navikt/aksel-icons'
import React from 'react'

export function UnntakAktivitetspliktOppgaveForm(props: {
  unntak?: IAktivitetspliktUnntak
  onSuccess: () => void
  onAvbryt?: () => void
}) {
  const { control, register, handleSubmit } = useForm<Partial<IOpprettAktivitetspliktUnntak>>({
    defaultValues: props.unntak,
  })

  function sendInn(formdata: Partial<IOpprettAktivitetspliktUnntak>) {
    console.log(formdata)
    props.onSuccess()
  }

  return (
    <form onSubmit={handleSubmit(sendInn)}>
      <VStack gap="4">
        <HStack gap="4">
          <ControlledDatoVelger name="fom" label="Unntak fra og med" control={control} />
          <ControlledDatoVelger name="tom" label="Unntak til og med" required={false} control={control} />
        </HStack>
        <Select
          {...register('unntak', {
            required: {
              value: true,
              message: 'Du må velge typen unntak.',
            },
          })}
          label="Type unntak"
        >
          <option value={AktivitetspliktUnntakType.MIDLERTIDIG_SYKDOM}>
            {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.MIDLERTIDIG_SYKDOM]}
          </option>
          <option value={AktivitetspliktUnntakType.MANGLENDE_TILSYNSORDNING_SYKDOM}>
            {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.MANGLENDE_TILSYNSORDNING_SYKDOM]}
          </option>
          <option value={AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM}>
            {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM]}
          </option>
          <option value={AktivitetspliktUnntakType.OMSORG_BARN_UNDER_ETT_AAR}>
            {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.OMSORG_BARN_UNDER_ETT_AAR]}
          </option>
          <option value={AktivitetspliktUnntakType.SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE}>
            {tekstAktivitetspliktUnntakType[AktivitetspliktUnntakType.SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE]}
          </option>
        </Select>

        <Textarea {...register('beskrivelse')} label="Beskrivelse" />
        <HStack gap="4">
          {!!props.onAvbryt && (
            <Button variant="secondary" onClick={props.onAvbryt}>
              Avbryt
            </Button>
          )}
          <Button variant="primary" type="submit" icon={<FloppydiskIcon />}>
            Lagre
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
