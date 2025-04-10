import { BodyLong, BodyShort, Box, Button, Heading, HStack, Textarea, VStack } from '@navikt/ds-react'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { DocPencilIcon, FloppydiskIcon, PencilIcon, PlusIcon, TrashIcon, XMarkIcon } from '@navikt/aksel-icons'

interface Props {
  redigerbar: boolean
  trygdetid: ITrygdetid
  oppdaterTrygdetidBegrunnelse: (begrunnelse: string | undefined) => void
}

export const BegrunnelseForTrygdetid = ({ redigerbar, trygdetid, oppdaterTrygdetidBegrunnelse }: Props) => {
  const { register, getValues, reset, handleSubmit } = useForm<{ begrunnelse: string | undefined }>({
    defaultValues: {
      begrunnelse: trygdetid.begrunnelse,
    },
  })

  useEffect(() => {
    reset({ begrunnelse: trygdetid.begrunnelse })
  }, [trygdetid])

  const [redigerBegrunnelse, setRedigerBegrunnelse] = useState<boolean>(false)

  return (
    <form
      onSubmit={handleSubmit((data) => {
        oppdaterTrygdetidBegrunnelse(data.begrunnelse)
        setRedigerBegrunnelse(false)
      })}
    >
      <VStack gap="4">
        <HStack gap="2" align="center">
          <DocPencilIcon aria-hidden height="1.5rem" width="1.5rem" />
          <Heading size="small" level="3">
            Begrunnelse for trygdetid
          </Heading>
        </HStack>
        {!redigerBegrunnelse && (
          <>
            {redigerbar && (
              <VStack gap="4">
                {getValues().begrunnelse ? (
                  <BodyLong style={{ whiteSpace: 'pre-line' }}>{getValues().begrunnelse}</BodyLong>
                ) : (
                  <BodyShort>Ingen begrunnelse oppgitt</BodyShort>
                )}
                <HStack gap="4">
                  <Button
                    type="button"
                    variant="secondary"
                    size="small"
                    icon={getValues().begrunnelse ? <PencilIcon aria-hidden /> : <PlusIcon aria-hidden />}
                    onClick={() => setRedigerBegrunnelse(true)}
                  >
                    {getValues().begrunnelse ? 'Rediger' : 'Legg til'}
                  </Button>
                  {getValues().begrunnelse && (
                    <Button
                      type="button"
                      variant="secondary"
                      size="small"
                      icon={<TrashIcon aria-hidden />}
                      onClick={() => {
                        reset({ begrunnelse: undefined })
                        oppdaterTrygdetidBegrunnelse(undefined)
                      }}
                    >
                      Slett
                    </Button>
                  )}
                </HStack>
              </VStack>
            )}
          </>
        )}
        {redigerbar && redigerBegrunnelse && (
          <>
            <Box width="35rem">
              <Textarea
                {...register('begrunnelse')}
                description="Beskriv og vurder registrert trygdetid. Begrunn dersom det er gjort endring av trygdetid fra tidligere behandling."
                autoComplete="off"
                minRows={3}
                label=""
              />
            </Box>
            <HStack gap="4">
              <Button
                type="button"
                variant="secondary"
                size="small"
                icon={<XMarkIcon aria-hidden />}
                onClick={() => {
                  setRedigerBegrunnelse(false)
                  reset()
                }}
              >
                Avbryt
              </Button>
              <Button size="small" icon={<FloppydiskIcon aria-hidden />}>
                Lagre
              </Button>
            </HStack>
          </>
        )}
      </VStack>
    </form>
  )
}
