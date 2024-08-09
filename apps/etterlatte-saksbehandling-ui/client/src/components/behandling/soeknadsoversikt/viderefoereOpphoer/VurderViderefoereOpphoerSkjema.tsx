import { Button, HStack, Radio, Select, Textarea, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { JaNei } from '~shared/types/ISvar'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { Vilkaartype } from '~shared/api/vilkaarsvurdering'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'

interface Props {
  viderefoereOpphoer: ViderefoertOpphoer | null
  vilkaartyper: Vilkaartype[]
  oppdaterViderefoereOpphoer: (viderefoereOpphoer: ViderefoertOpphoer) => void
  setVisVurderKnapp: (visVurderKnapp: boolean) => void
}

export const VurderViderefoereOpphoerSkjema = ({ viderefoereOpphoer, vilkaartyper }: Props) => {
  const {
    register,
    control,
    watch,
    formState: { errors },
  } = useForm<ViderefoertOpphoer>({ defaultValues: viderefoereOpphoer !== null ? viderefoereOpphoer : undefined })

  return (
    <VStack gap="4" paddingBlock="0 3">
      <ControlledRadioGruppe
        name="skalViderefoere"
        control={control}
        legend="Skal viderefoeres?"
        radios={
          <HStack gap="4">
            <Radio value={JaNei.JA}>Ja</Radio>
            <Radio value={JaNei.NEI}>Nei</Radio>
          </HStack>
        }
        errorVedTomInput="Må settes"
      />
      {watch().skalViderefoere === JaNei.JA && (
        <>
          <ControlledMaanedVelger name="dato" label="Opphørstidspunkt" control={control} required />
          <Select
            {...register('vilkaar', {
              required: {
                value: true,
                message: 'Du må velge et vilkår',
              },
            })}
            label="Velg vilkåret som gjør at saken opphører"
            error={errors.vilkaar?.message}
          >
            <option value="">Velg et vilkår</option>
            {vilkaartyper.map((type: Vilkaartype) => (
              <option key={type.name} value={type.name}>
                {type.tittel}
              </option>
            ))}
          </Select>
        </>
      )}
      <Textarea {...register('begrunnelse')} label="Begrunnelse" placeholder="Valgfritt" />
      <HStack gap="4">
        <Button size="small" icon={<FloppydiskIcon aria-hidden />}>
          Lagre
        </Button>
        <Button type="button" variant="secondary" size="small" icon={<XMarkIcon aria-hidden />}>
          Avbryt
        </Button>
      </HStack>
    </VStack>
  )
}
