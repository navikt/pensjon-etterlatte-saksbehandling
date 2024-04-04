import { Button, Heading, Modal, Select } from '@navikt/ds-react'
import { DocPencilIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettBrevAvSpesifikkTypeForSak } from '~shared/api/brev'
import { useForm } from 'react-hook-form'
import { FlexRow } from '~shared/styled'
import { isPending } from '~shared/api/apiUtils'
import { useNavigate } from 'react-router-dom'

export const NyttBrevModal = ({ sakId }: { sakId: number }) => {
  const [opprettBrevStatus, opprettBrevApiCall] = useApiCall(opprettBrevAvSpesifikkTypeForSak)
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const defaultData: FilledFormData = {
    type: 'TOMT_BREV',
  }

  const {
    formState: { errors },
    handleSubmit,
    watch,
    register,
  } = useForm({ defaultValues: defaultData })

  const skjemaet = watch()

  const opprettBrev = (formData: FilledFormData) => {
    const brevParametre = mapFormdataToBrevParametre(formData)

    opprettBrevApiCall({ sakId: sakId, body: brevParametre }, (brev) => {
      setOpen(false)
      navigate(`/person/${brev.soekerFnr}/sak/${brev.sakId}/brev/${brev.id}`)
    })
  }

  const avbryt = () => {
    setOpen(false)
  }

  return (
    <>
      <Button
        variant="primary"
        icon={<DocPencilIcon />}
        iconPosition="right"
        size="small"
        onClick={() => setOpen(true)}
      >
        Nytt brev
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Header>
          <Heading size="medium" id="modal-heading">
            Lag nytt brev
          </Heading>
        </Modal.Header>

        <form onSubmit={handleSubmit(opprettBrev)}>
          <Modal.Body>
            <FlexRow $spacing>
              <Select
                error={errors?.type?.message}
                label="Type"
                {...register('type', {
                  required: { value: true, message: 'Feltet er påkrevd' },
                })}
              >
                <option value="OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_VARSELBREV">Varselbrev om aktivitetsplikt</option>
                <option value="TOMT_BREV">Manuelt brev</option>
              </Select>
            </FlexRow>

            {skjemaet.type === 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_VARSELBREV' && (
              <FlexRow $spacing>
                <Select
                  error={errors?.aktivitetsgrad?.message}
                  label="Aktivitetsgrad"
                  {...register('aktivitetsgrad', {
                    required: { value: true, message: 'Du må velge aktivitetsgrad' },
                    validate: { notDefault: (value) => !!value },
                  })}
                >
                  <option value="">Velg aktivitetsgrad</option>
                  <option value="IKKE_I_AKTIVITET">Ikke i aktivitet</option>
                  <option value="UNDER_50_PROSENT">Under 50%</option>
                  <option value="OVER_50_PROSENT">Over 50%</option>
                </Select>
              </FlexRow>
            )}
          </Modal.Body>

          <Modal.Footer>
            <FlexRow justify="right">
              <Button variant="secondary" type="button" disabled={isPending(opprettBrevStatus)} onClick={avbryt}>
                Avbryt
              </Button>
              <Button variant="primary" type="submit" loading={isPending(opprettBrevStatus)}>
                Opprett brev
              </Button>
            </FlexRow>
          </Modal.Footer>
        </form>
      </Modal>
    </>
  )
}

export type BrevParametre =
  | {
      type: 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_VARSELBREV'
      aktivitetsgrad: string
    }
  | {
      type: 'TOMT_BREV'
    }

type FilledFormData = {
  type: string
  aktivitetsgrad?: string
}

function mapFormdataToBrevParametre(formdata: FilledFormData): BrevParametre {
  switch (formdata.type) {
    case 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_VARSELBREV':
      return {
        type: formdata.type,
        aktivitetsgrad: formdata.aktivitetsgrad!!,
      }
    case 'TOMT_BREV':
      return {
        type: formdata.type,
      }
    default:
      throw new Error('Valgt type er ikke gyldig')
  }
}
