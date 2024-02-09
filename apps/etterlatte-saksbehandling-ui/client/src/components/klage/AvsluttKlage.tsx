import React, { useState } from 'react'
import { Button, Heading, Modal, Select, Textarea } from '@navikt/ds-react'
import { useNavigate } from 'react-router'
import { useApiCall } from '~shared/hooks/useApiCall'
import { TrashIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { FlexRow } from '~shared/styled'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { avsluttKlage } from '~shared/api/klage'
import { useKlage, useKlageRedigerbar } from '~components/klage/useKlage'
import { Controller, useForm } from 'react-hook-form'
import { AarsakTilAvslutting, AvsluttKlageRequest, teksterAarsakTilAvslutting } from '~shared/types/Klage'
import { SidebarPanel } from '~shared/components/Sidebar'

export default function AvsluttKlage() {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [showAvsluttForm, setShowAvsluttForm] = useState(false)
  const [avsluttKlageStatus, avsluttKlagen] = useApiCall(avsluttKlage)

  const klage = useKlage()
  const klageRedigerbar = useKlageRedigerbar()

  if (!klage || !klageRedigerbar) {
    return null
  }

  const {
    control,
    handleSubmit,
    trigger,
    reset,
    watch,
    formState: { errors },
  } = useForm<AvsluttKlageRequest>({
    defaultValues: { klageId: klage.id, aarsakTilAvbrytelse: AarsakTilAvslutting.ANNET, kommentar: '' },
  })

  const avslutt = (request: AvsluttKlageRequest) => {
    avsluttKlagen(request, () => {
      setIsOpen(false)
      navigate(`/person/${klage?.sak?.ident}`)
    })
  }

  return (
    klage &&
    klageRedigerbar && (
      <>
        {!showAvsluttForm && (
          <SidebarPanel>
            <FlexRow>
              <Button variant="secondary" icon={<TrashIcon />} onClick={() => setShowAvsluttForm(!showAvsluttForm)}>
                Avslutt sak
              </Button>
            </FlexRow>
          </SidebarPanel>
        )}
        {showAvsluttForm && (
          <SidebarPanel border>
            <AvsluttKlageForm />
          </SidebarPanel>
        )}
        <BekreftelseModal />
      </>
    )
  )

  function AvsluttKlageForm() {
    return (
      <AvsluttForm id="avslutt-klage-form" onSubmit={handleSubmit(avslutt)}>
        <Controller
          name="aarsakTilAvbrytelse"
          rules={{
            required: { value: true, message: 'Du må velge en årsak for omgjøringen.' },
            minLength: 1,
          }}
          control={control}
          render={({ field }) => {
            const { value, ...rest } = field
            return (
              <>
                <Select label="Årsak til avslutning" value={value ?? ''} {...rest}>
                  {Object.entries(AarsakTilAvslutting).map(([key, value]) => (
                    <option key={key} value={key}>
                      {teksterAarsakTilAvslutting[value]}
                    </option>
                  ))}
                </Select>
              </>
            )
          }}
        />
        <Controller
          name="kommentar"
          rules={{
            validate: (value, formValues) => {
              return formValues.aarsakTilAvbrytelse == 'ANNET' && value.length === 0
                ? 'Du må skrive en kommentar'
                : true
            },
          }}
          control={control}
          render={({ field }) => {
            const { value, ...rest } = field
            return (
              <>
                <Textarea
                  label={`Begrunnelse (${watch('aarsakTilAvbrytelse') === 'ANNET' ? 'obligatorisk' : 'valgfritt'})`}
                  description="Utdyp hvorfor klagebehandlingen avsluttes"
                  value={value ?? ''}
                  {...rest}
                  size="medium"
                  error={errors?.kommentar?.message}
                />
              </>
            )
          }}
        />
        <FlexRow>
          <Button
            type="button"
            size="small"
            variant="danger"
            onClick={() => trigger().then((success) => success && setIsOpen(true))}
          >
            Avslutt sak
          </Button>
          <Button type="button" size="small" variant="tertiary" onClick={() => avbrytAvslutting}>
            Avbryt
          </Button>
        </FlexRow>
      </AvsluttForm>
    )
  }

  function BekreftelseModal() {
    return (
      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Header>
          <Heading level="1" spacing size="medium" id="modal-heading">
            <TrashIcon />
            Avslutt sak
          </Heading>
        </Modal.Header>
        <Modal.Body>
          Er du sikker på at du vil avslutte saken? Den vil da få status Avsluttet.
          {isFailureHandler({
            apiResult: avsluttKlageStatus,
            errorMessage: 'Det oppsto en feil ved avslutning av saken',
          })}
        </Modal.Body>

        <Modal.Footer>
          <Button
            type="submit"
            form="avslutt-klage-form"
            variant="danger"
            loading={isPending(avsluttKlageStatus)}
            style={{ marginLeft: 'auto' }}
          >
            Avslutt sak
          </Button>

          <Button
            variant="tertiary"
            onClick={() => avbrytAvslutting}
            loading={isPending(avsluttKlageStatus)}
            style={{ marginLeft: 0 }}
          >
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    )
  }

  function avbrytAvslutting() {
    reset()
    setIsOpen(false)
    setShowAvsluttForm(false)
  }
}

const AvsluttForm = styled.form`
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 2em;
  margin-bottom: 2em;
`
