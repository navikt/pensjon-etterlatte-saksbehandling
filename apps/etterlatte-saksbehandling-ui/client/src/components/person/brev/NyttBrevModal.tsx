import { Button, Heading, Modal, Select } from '@navikt/ds-react'
import { DocPencilIcon } from '@navikt/aksel-icons'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettBrevAvSpesifikkTypeForSak } from '~shared/api/brev'
import { Controller, useForm } from 'react-hook-form'
import { FlexRow } from '~shared/styled'
import { isPending } from '~shared/api/apiUtils'

export const NyttBrevModal = ({ sakId }: { sakId: number }) => {
  const [nyttBrevStatus, opprettBrev] = useApiCall(opprettBrevAvSpesifikkTypeForSak)
  const [open, setOpen] = useState(false)

  const defaultData: FilledFormData = {
    type: 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_VARSELBREV',
    aktivitetsgrad: '100%',
  }

  const {
    formState: { isDirty, errors },
    handleSubmit,
    control,
    watch,
  } = useForm({ defaultValues: defaultData })

  const heleSkjemaet = watch()

  const lagre = (formData: FilledFormData) => {
    if (!isDirty) {
      setOpen(false)
      return
    }
    const brevParametre = mapFormdataToBrevParametre(formData)

    opprettBrev({ sakId: sakId, body: brevParametre }, () => {
      setOpen(false)
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

        <form onSubmit={handleSubmit((data) => lagre(data))}>
          <Modal.Body>
            <FlexRow $spacing>
              <Controller
                rules={{
                  required: true,
                }}
                name="type"
                control={control}
                render={({ field }) => (
                  <Select {...field} label="Type" error={errors.type?.message}>
                    <option value="OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_VARSELBREV">Varselbrev om aktivitetsplikt</option>
                    <option value="TOMT_BREV">Tomt brev</option>
                  </Select>
                )}
              />
            </FlexRow>

            {heleSkjemaet.type === 'OMSTILLINGSSTOENAD_AKTIVITETSPLIKT_VARSELBREV' && (
              <FlexRow $spacing>
                <Controller
                  rules={{
                    required: true,
                  }}
                  name="aktivitetsgrad"
                  control={control}
                  render={({ field }) => (
                    <Select {...field} label="Aktivitetsgrad" error={errors.aktivitetsgrad?.message}>
                      <option value="IKKE_I_AKTIVITET">Ikke i aktivitet</option>
                      <option value="UNDER_50_PROSENT">Under 50%</option>
                      <option value="OVER_50_PROSENT">Over 50%</option>
                    </Select>
                  )}
                />
              </FlexRow>
            )}
          </Modal.Body>

          <Modal.Footer>
            <FlexRow justify="right">
              <Button variant="secondary" type="button" disabled={isPending(nyttBrevStatus)} onClick={avbryt}>
                Avbryt
              </Button>
              <Button variant="primary" type="submit" loading={isPending(nyttBrevStatus)}>
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
