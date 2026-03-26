import { BodyShort, Heading, Label, VStack } from '@navikt/ds-react'
import { useEtteroppgjoerForbehandling } from '~store/reducers/EtteroppgjoerReducer'
import { JaNei } from '~shared/types/ISvar'

interface Props {
  erRedigerbar: boolean
}

export const AktivitetspliktVisning = ({ erRedigerbar }: Props) => {
  const { forbehandling } = useEtteroppgjoerForbehandling()

  if (!forbehandling.aktivitetspliktOverholdt) {
    if (!erRedigerbar) {
      return (
        <VStack gap="2">
          <Label>Aktivitetsplikt</Label>
          <BodyShort>
            Spørsmålet om aktivitetsplikt ble lagt til i ettertid, og er ikke svart på i denne behandlingen.
          </BodyShort>
        </VStack>
      )
    }
    return <Heading size="small">Spørsmål om aktivitetsplikt er ikke besvart</Heading>
  }

  return (
    <VStack gap="4">
      <VStack gap="2">
        <Label>Er aktivitetsplikten overholdt i etteroppgjørsåret?</Label>
        <BodyShort>{forbehandling.aktivitetspliktOverholdt === JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
      </VStack>
      {forbehandling.aktivitetspliktBegrunnelse && (
        <VStack gap="2" maxWidth="30rem">
          <Label>Begrunnelse</Label>
          <BodyShort>{forbehandling.aktivitetspliktBegrunnelse}</BodyShort>
        </VStack>
      )}
    </VStack>
  )
}
