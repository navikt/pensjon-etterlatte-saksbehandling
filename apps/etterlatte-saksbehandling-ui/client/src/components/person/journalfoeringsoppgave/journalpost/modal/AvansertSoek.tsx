import React, { Dispatch, SetStateAction, useEffect } from 'react'
import { Button, Heading, Modal, Select, TextField } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleLand } from '~shared/api/trygdetid'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'

interface ModalProps {
  open: boolean
  setOpen: Dispatch<SetStateAction<boolean>>
}

export interface SoekPerson {
  fornavn: string
  etternavn: string
  statsborgerskap?: string
  foedselsdato?: Date
  doedsdato?: string
}

export default function AvansertSoek({ open, setOpen }: ModalProps) {
  const [landListeResult, landListeFetch] = useApiCall(hentAlleLand)
  useEffect(() => {
    landListeFetch(null)
  }, [])

  const navnErGyldig = (navn: string): boolean => {
    const utenMellomrom = navn.trim()
    const spesicalCharacters = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]+/
    return utenMellomrom.match(/\d+/) == null && utenMellomrom.match(spesicalCharacters) == null
  }

  const {
    getValues,
    register,
    control,
    formState: { errors },
    handleSubmit,
    watch,
  } = useForm<SoekPerson>({ defaultValues: { statsborgerskap: undefined }, mode: 'all' })
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
          {...register('fornavn', {
            validate: (fornavn) => navnErGyldig(fornavn) || 'Fornavn kan kun bestå av bokstaver',
          })}
          label="Fornavn"
          error={errors?.fornavn?.message}
        />
        <TextField
          {...register('etternavn', {
            validate: (etternavn) => navnErGyldig(etternavn) || 'Etternavn kan kun bestå av bokstaver',
          })}
          label="Etternavn"
          error={errors?.etternavn?.message}
        />
        {mapResult(landListeResult, {
          pending: <Spinner label="Hent landliste" visible />,
          error: (error) => (
            <ApiErrorAlert>
              {error.detail || 'Feil ved henting av landliste. Kan ikke søke med statsborgerskap.'}
            </ApiErrorAlert>
          ),
          success: (landListe) => (
            <Select label="Velg statsborgerskap" {...register('statsborgerskap', {})}>
              <option disabled>Velg land</option>
              {landListe.map((land) => (
                <option key={land.isoLandkode} value={land.isoLandkode}>
                  {land.beskrivelse.tekst}
                </option>
              ))}
            </Select>
          ),
        })}
        <ControlledDatoVelger name="foedselsdato" label="Fødselsdato" control={control} />
        <ControlledDatoVelger name="doedsdato" label="Dødsdato" control={control} />
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={() => setOpen(false)}>
          Avbryt søk
        </Button>
        <Button
          variant="primary"
          onClick={() => {
            handleSubmit(
              (person) => {
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
