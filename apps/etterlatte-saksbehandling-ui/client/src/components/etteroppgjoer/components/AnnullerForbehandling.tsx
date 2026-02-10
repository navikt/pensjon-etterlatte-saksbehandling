import React, { useState } from 'react'
import {
  BodyLong,
  Box,
  Button,
  ExpansionCard,
  Heading,
  HStack,
  Modal,
  Select,
  Textarea,
  VStack,
} from '@navikt/ds-react'
import { useNavigate } from 'react-router'
import { ExclamationmarkTriangleFillIcon, XMarkIcon } from '@navikt/aksel-icons'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Controller, useForm } from 'react-hook-form'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import {
  AarsakTilAvsluttingEtteroppgjoerForbehandling,
  AvbrytEtteroppgjoerForbehandlingRequest,
  EtteroppgjoerForbehandlingStatus,
  tekstAarsakTilAvsluttingEtteroppgjoerForbehandling,
} from '~shared/types/EtteroppgjoerForbehandling'
import { avbrytEtteroppgjoerForbehandling } from '~shared/api/etteroppgjoer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export default function AnnullerForbehandling() {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [status, avbrytBehandling] = useApiCall(avbrytEtteroppgjoerForbehandling)
  const { forbehandling } = useEtteroppgjoerForbehandling()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const kanRedigeres =
    ![EtteroppgjoerForbehandlingStatus.FERDIGSTILT, EtteroppgjoerForbehandlingStatus.AVBRUTT].includes(
      forbehandling.status
    ) && enhetErSkrivbar(forbehandling.sak.enhet, innloggetSaksbehandler.skriveEnheter)

  const {
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<AvbrytEtteroppgjoerForbehandlingRequest>({
    defaultValues: { aarsakTilAvbrytelse: AarsakTilAvsluttingEtteroppgjoerForbehandling.ANNET, kommentar: '' },
  })

  if (!kanRedigeres) {
    return null
  }

  const annuller = (data: AvbrytEtteroppgjoerForbehandlingRequest) => {
    avbrytBehandling({ id: forbehandling!!.id, avbrytEtteroppgjoerForbehandlingRequest: data }, () => {
      if (forbehandling.sak.ident) {
        navigate('/person', { state: { fnr: forbehandling.sak.ident } })
      } else {
        window.location.reload() // Bare refresh behandling
      }
    })
  }

  return (
    <Box paddingInline="space-2" paddingBlock="space-4">
      <ExpansionCard aria-labelledby="card-heading" size="small">
        <ExpansionCard.Header>
          <HStack wrap={false} gap="space-4" align="center">
            <ExclamationmarkTriangleFillIcon aria-hidden />
            <div>
              <ExpansionCard.Title size="small">Annuller forbehandling</ExpansionCard.Title>
            </div>
          </HStack>
        </ExpansionCard.Header>

        <ExpansionCard.Content>
          <BodyLong>Her kan du avslutte forbehandlingen</BodyLong>
          <br />
          <div className="flex">
            <Button
              data-color="danger"
              size="small"
              variant="primary"
              onClick={() => setIsOpen(true)}
              icon={<XMarkIcon aria-hidden />}
            >
              Annuller forbehandling
            </Button>
          </div>
        </ExpansionCard.Content>
      </ExpansionCard>
      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Header>
          <Heading level="1" spacing size="medium" id="modal-heading">
            Er du sikker på at du vil annullere forbehandlingen for etteroppgjøret?
          </Heading>
        </Modal.Header>

        <Modal.Body>
          <VStack gap="space-4">
            <BodyLong>
              Denne forbehandlingen blir annullert og må opprettes på nytt for å fullføre etteroppgjøret
            </BodyLong>

            <>
              <Controller
                name="aarsakTilAvbrytelse"
                rules={{
                  required: { value: true, message: 'Du må velge en årsak for annulleringen.' },
                  minLength: 1,
                }}
                control={control}
                render={({ field }) => {
                  const { value, ...rest } = field
                  return (
                    <>
                      <Select label="Årsak til avslutning" value={value ?? ''} {...rest}>
                        {Object.values(AarsakTilAvsluttingEtteroppgjoerForbehandling).map((aarsak) => (
                          <option key={aarsak} value={aarsak}>
                            {tekstAarsakTilAvsluttingEtteroppgjoerForbehandling[aarsak]}
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
                        description="Utdyp hvorfor behandlingen avsluttes"
                        value={value ?? ''}
                        {...rest}
                        size="medium"
                        error={errors?.kommentar?.message}
                      />
                    </>
                  )
                }}
              />
            </>
          </VStack>

          {isFailureHandler({
            apiResult: status,
            errorMessage: 'Det oppsto en feil ved avbryting av forbehandlingen.',
          })}
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="space-4" justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)} loading={isPending(status)}>
              Nei, fortsett forbehandlingen
            </Button>
            <Button data-color="danger" variant="primary" onClick={handleSubmit(annuller)} loading={isPending(status)}>
              Ja, annuller forbehandlingen
            </Button>
          </HStack>
        </Modal.Footer>
      </Modal>
    </Box>
  )
}
