import { useState } from 'react'
import { Alert, BodyShort, Button, Heading, Link, VStack } from '@navikt/ds-react'
import { FieldErrors } from 'react-hook-form'
import { PencilIcon } from '@navikt/aksel-icons'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { AktivitetspliktSkjema } from '~components/etteroppgjoer/components/aktivitetsplikt/AktivitetspliktSkjema'
import { AktivitetspliktVisning } from '~components/etteroppgjoer/components/aktivitetsplikt/AktivitetspliktVisning'
import { Aktivitetsplikt } from '~shared/types/EtteroppgjoerForbehandling'
import { JaNei } from '~shared/types/ISvar'

interface Props {
  erRedigerbar: boolean
  setAktivitetspliktSkjemaErrors: (errors: FieldErrors<Aktivitetsplikt> | undefined) => void
}

export const AktivitetspliktSpørsmål = ({ erRedigerbar, setAktivitetspliktSkjemaErrors }: Props) => {
  const { forbehandling } = useEtteroppgjoerForbehandling()

  const [aktivitetspliktSkjemaErAapen, setAktivitetspliktSkjemaErAapen] = useState<boolean>(
    erRedigerbar && !forbehandling.aktivitetspliktOverholdt
  )

  return (
    <VStack gap="4">
      <Heading size="large">Aktivitetsplikt</Heading>
      <BodyShort>
        Vurder om aktivitetsplikten er overholdt i etteroppgjørsåret.{' '}
        <Link href="https://lovdata.no/pro/lov/1997-02-28-19/§17-7" target="_blank">
          Se folketrygdloven § 17-7
        </Link>
      </BodyShort>

      {aktivitetspliktSkjemaErAapen && erRedigerbar ? (
        <AktivitetspliktSkjema
          setAktivitetspliktSkjemaErAapen={setAktivitetspliktSkjemaErAapen}
          erRedigerbar={erRedigerbar}
          setAktivitetspliktSkjemaErrors={setAktivitetspliktSkjemaErrors}
        />
      ) : (
        <VStack gap="4">
          <AktivitetspliktVisning erRedigerbar={erRedigerbar} />
          {erRedigerbar && (
            <div>
              <Button
                size="small"
                variant="secondary"
                icon={<PencilIcon aria-hidden />}
                onClick={() => setAktivitetspliktSkjemaErAapen(true)}
              >
                Rediger
              </Button>
            </div>
          )}
        </VStack>
      )}

      {forbehandling.aktivitetspliktOverholdt === JaNei.NEI && (
        <Alert variant="warning">
          Aktivitetsplikten er ikke overholdt. Etteroppgjøret kan ikke gjennomføres før aktivitetsplikten for
          etteroppgjørsåret er vurdert på nytt.
        </Alert>
      )}
    </VStack>
  )
}
