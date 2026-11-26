import { Textarea, VStack } from '@navikt/ds-react'
import { useForlengetOms } from './ForlengetOmsContext'

export const ForlengetOmsIngenAktivitet = () => {
  const { utkast, oppdaterFelt, sporsmaalRedigerbare } = useForlengetOms()

  return (
    <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-32">
      <VStack gap="space-16" maxWidth="30rem">
        <Textarea
          label="Begrunnelse"
          value={utkast.begrunnelse}
          onChange={(event) => oppdaterFelt('begrunnelse', event.target.value)}
          minRows={6}
          readOnly={!sporsmaalRedigerbare}
        />
      </VStack>
    </VStack>
  )
}
