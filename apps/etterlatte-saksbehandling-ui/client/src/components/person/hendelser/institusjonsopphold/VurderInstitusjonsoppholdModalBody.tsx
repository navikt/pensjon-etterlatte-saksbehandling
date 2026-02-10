import { Button, HStack, Modal, Radio, Textarea, VStack } from '@navikt/ds-react'
import React, { Dispatch, SetStateAction } from 'react'
import { InstitusjonsoppholdReadMore } from '~components/person/hendelser/institusjonsopphold/InstitusjonsoppholdReadMore'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { JaNei } from '~shared/types/ISvar'
import { isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreInstitusjonsoppholdData } from '~shared/api/behandling'

export interface InstitusjonsoppholdBegrunnelse {
  kanGiReduksjonAvYtelse: JaNei
  kanGiReduksjonAvYtelseBegrunnelse: string
  forventetVarighetMerEnn3Maaneder: JaNei
  forventetVarighetMerEnn3MaanederBegrunnelse: string
  grunnlagsEndringshendelseId: string
}

interface Props {
  setOpen: Dispatch<SetStateAction<boolean>>
  sakId: number
  hendelseId: string
  arkiverHendelse: () => void
}

export const VurderInstitusjonsoppholdModalBody = ({ setOpen, sakId, hendelseId, arkiverHendelse }: Props) => {
  const [lagreInstitusjonsoppholdResult, lagreInstitusjonsopphold] = useApiCall(lagreInstitusjonsoppholdData)

  const {
    register,
    control,
    reset,
    handleSubmit,
    formState: { errors },
  } = useForm<InstitusjonsoppholdBegrunnelse>()

  const lukkModal = () => {
    reset()
    setOpen(false)
  }

  const vurderInstitusjonsopphold = (data: InstitusjonsoppholdBegrunnelse) => {
    lagreInstitusjonsopphold(
      {
        sakId: sakId,
        institusjonsopphold: {
          ...data,
          grunnlagsEndringshendelseId: hendelseId,
        },
      },
      () => {
        arkiverHendelse()
      }
    )
  }

  return (
    <Modal.Body>
      <VStack gap="space-8">
        <InstitusjonsoppholdReadMore />

        <VStack gap="space-4">
          <ControlledRadioGruppe
            name="kanGiReduksjonAvYtelse"
            control={control}
            legend="Er dette en institusjon som kan gi reduksjon av ytelsen?"
            errorVedTomInput="Du må sette om institusjon kan gi reduksjon av ytelsen"
            radios={
              <>
                <Radio value={JaNei.JA}>Ja</Radio>
                <Radio value={JaNei.NEI}>Nei</Radio>
              </>
            }
          />
          <Textarea
            {...register('kanGiReduksjonAvYtelseBegrunnelse', {
              required: {
                value: true,
                message: 'Du må sette begrunnelse for reduksjon',
              },
            })}
            label="Begrunnelse for reduksjon"
            error={errors.kanGiReduksjonAvYtelseBegrunnelse?.message}
          />
        </VStack>

        <VStack gap="space-4">
          <ControlledRadioGruppe
            name="forventetVarighetMerEnn3Maaneder"
            control={control}
            legend="Er oppholdet forventet å vare lenger enn innleggelsesmåned + tre måneder?"
            errorVedTomInput="Du må sette om oppholdet er lengre enn innleggelsemåned + tre måneder"
            radios={
              <>
                <Radio value={JaNei.JA}>Ja</Radio>
                <Radio value={JaNei.NEI}>Nei</Radio>
              </>
            }
          />
          <Textarea
            {...register('forventetVarighetMerEnn3MaanederBegrunnelse', {
              required: {
                value: true,
                message: 'Du må sette begrunnelse for varighet',
              },
            })}
            label="Begrunnelse for varighet"
            error={errors.forventetVarighetMerEnn3MaanederBegrunnelse?.message}
          />
        </VStack>

        <HStack gap="space-2" justify="end">
          <Button variant="secondary" type="button" onClick={lukkModal}>
            Avbryt
          </Button>
          <Button loading={isPending(lagreInstitusjonsoppholdResult)} onClick={handleSubmit(vurderInstitusjonsopphold)}>
            Vurder
          </Button>
        </HStack>
      </VStack>
    </Modal.Body>
  )
}
