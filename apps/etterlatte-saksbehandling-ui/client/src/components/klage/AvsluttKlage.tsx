import React, { useState } from 'react'
import { Button, ExpansionCard, Heading, Modal, Select, Textarea } from '@navikt/ds-react'
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

export default function AvsluttKlage() {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
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
    <>
      <ExpansionCardSpaced aria-labelledby="card-heading">
        <ExpansionCard.Header>
          <div className="with-icon">
            <div>
              <Button variant="secondary" icon={<TrashIcon />}>
                Avslutt sak
              </Button>
            </div>
          </div>
        </ExpansionCard.Header>

        <ExpansionCard.Content>
          <AvsluttForm id="avslutt-klage-form" onSubmit={handleSubmit(avslutt)}>
            <FlexRow>
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
                      <Select label="Årsak til avslutting" value={value ?? ''} {...rest}>
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
            </FlexRow>
            <Controller
              name="kommentar"
              rules={{
                validate: (value, formValues) => {
                  return formValues.aarsakTilAvbrytelse == 'ANNET' && value.length == 0
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
                      label="Begrunnelse"
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
            </FlexRow>
          </AvsluttForm>
        </ExpansionCard.Content>
      </ExpansionCardSpaced>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Header>
          <Heading level="1" spacing size="medium" id="modal-heading">
            <TrashIcon />
            Avslutt sak
          </Heading>
        </Modal.Header>
        <Modal.Body>Er du sikker på at du vil avslutte klagen? Klagen vil få status avsluttet.</Modal.Body>

        <Modal.Footer>
          <FlexRow justify="center">
            <Button variant="tertiary" onClick={() => setIsOpen(false)} loading={isPending(avsluttKlageStatus)}>
              Nei, fortsett behandlingen
            </Button>
            <Button type="submit" form="avslutt-klage-form" variant="danger" loading={isPending(avsluttKlageStatus)}>
              Ja, avslutt sak
            </Button>
          </FlexRow>
          {isFailureHandler({
            apiResult: avsluttKlageStatus,
            errorMessage: 'Det oppsto en feil ved avslutting av saken.',
          })}
        </Modal.Footer>
      </Modal>
    </>
  )
}

const ExpansionCardSpaced = styled(ExpansionCard)`
  margin: 20px 8px 0 8px;
  border-radius: 3px;

  .title {
    white-space: nowrap;
  }

  .navds-expansioncard__header {
    border-radius: 3px;
  }

  .with-icon {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .icon {
    font-size: 2rem;
    flex-shrink: 0;
    display: grid;
    place-content: center;
  }
`

const AvsluttForm = styled.form`
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 2em;
  margin-bottom: 2em;
`
