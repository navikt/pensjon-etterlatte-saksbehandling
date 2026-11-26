import { Alert, HGrid, MonthPicker, TextField, useMonthpicker, VStack } from '@navikt/ds-react'
import { format } from 'date-fns'
import { ReactNode } from 'react'
import { formaterMaanedAar } from '~utils/formatering/dato'
import { useForlengetOms } from './ForlengetOmsContext'

export const ForlengetOmsPeriode = ({ children }: { children: ReactNode }) => {
  const { periode, settPeriode, periodeFeil, periodeRedigerbar, avslagErLagret, vilkaarsvurdering } = useForlengetOms()
  const { fom, tom } = periode
  const { monthpickerProps, inputProps } = useMonthpicker({
    defaultSelected: tom ? new Date(`${tom}-01`) : undefined,
    fromDate: fom ? new Date(`${fom}-01`) : undefined,
    locale: 'nb',
    inputFormat: 'MMMM yyyy',
    onMonthChange: (dato) => settPeriode((forrige) => ({ ...forrige, tom: dato ? format(dato, 'yyyy-MM') : '' })),
    onValidate: (validering) => {
      if (validering.isEmpty) settPeriode((forrige) => ({ ...forrige, tom: '' }))
    },
  })

  return (
    <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-32" gap="space-24">
      {!avslagErLagret && (
        <VStack gap="space-16" maxWidth="30rem">
          <HGrid columns={2} gap="space-16">
            <TextField label="Fra dato" value={fom ? formaterMaanedAar(fom) : ''} readOnly />
            {periodeRedigerbar ? (
              <MonthPicker {...monthpickerProps}>
                <MonthPicker.Input {...inputProps} id="tom" label="Til dato" error={periodeFeil} />
              </MonthPicker>
            ) : (
              <TextField label="Til dato" value={tom ? formaterMaanedAar(tom) : ''} readOnly />
            )}
          </HGrid>
        </VStack>
      )}
      {children}
      {!vilkaarsvurdering?.resultat && (
        <Alert variant="info">
          Innvilgelse krever en fra og med- og til og med-måned. Perioden lagres sammen med utfallet. Avslag krever
          ingen periode.
        </Alert>
      )}
      {!vilkaarsvurdering && <Alert variant="info">Vilkårene må vurderes på vilkårsvurderingssiden først.</Alert>}
    </VStack>
  )
}
