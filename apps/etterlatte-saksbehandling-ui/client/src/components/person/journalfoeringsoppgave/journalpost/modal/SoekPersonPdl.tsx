import React, { Dispatch, SetStateAction } from 'react'
import { Button, Heading, Modal, TextField } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { soekPerson } from '~shared/api/pdltjenester'

interface ModalProps {
  open: boolean
  setOpen: Dispatch<SetStateAction<boolean>>
}

export interface SoekPerson {
  navn?: string
  foedselsdato?: Date
}

export default function SoekPersonPdl({ open, setOpen }: ModalProps) {
  const [, soekperson] = useApiCall(soekPerson)

  const navnErGyldig = (navn: string | undefined): boolean => {
    if (navn) {
      const utenMellomrom = navn.trim()
      const spesicalCharacters = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]+/
      return utenMellomrom.match(/\d+/) == null && utenMellomrom.match(spesicalCharacters) == null
    } else {
      return false
    }
  }

  const {
    getValues,
    register,
    control,
    formState: { errors },
    handleSubmit,
    watch,
  } = useForm<SoekPerson>({ defaultValues: {}, mode: 'all' })

  console.log('formstate: ', getValues())
  console.log('errors: ', errors)

  watch()
  return (
    <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)} width="medium">
      <Modal.Body>
        <Heading size="medium" id="modal-heading" spacing>
          Avansert Søk
        </Heading>
        <TextField
          {...register('navn', {
            required: { value: true, message: 'Navn er påkrevd i søket' },
            validate: (fornavn) => navnErGyldig(fornavn) || 'Fornavn kan kun bestå av bokstaver',
          })}
          label="Navn"
          error={errors?.navn?.message}
        />
        <ControlledDatoVelger name="foedselsdato" label="Fødselsdato" control={control} />
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={() => setOpen(false)}>
          Avbryt søk
        </Button>
        <Button
          variant="primary"
          onClick={() => {
            console.log('onclick')
            soekperson(getValues())
            handleSubmit(
              (person) => {
                soekperson(person)
                console.log('person: ', person)
              },
              (errors) => {
                console.log('errors:', errors)
              }
            )
          }}
        >
          Søk
        </Button>
      </Modal.Footer>
    </Modal>
  )
}
