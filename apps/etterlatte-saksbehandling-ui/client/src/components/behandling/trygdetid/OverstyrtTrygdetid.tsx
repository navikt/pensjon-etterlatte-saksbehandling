import { BodyShort, Box, Button, Heading, HStack, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useEffect, useState } from 'react'
import { SakType } from '~shared/types/sak'

interface Props {
  redigerbar: boolean
  sakType: SakType
  trygdetid: ITrygdetid
  virkningstidspunktEtterNyRegelDato: Boolean
  overstyrTrygdetidPoengaar: (trygdetid: ITrygdetid) => void
}

export const OverstyrtTrygdetid = ({
  redigerbar,
  sakType,
  trygdetid,
  virkningstidspunktEtterNyRegelDato,
  overstyrTrygdetidPoengaar,
}: Props) => {
  const [overstyrtNorskPoengaar, setOverstyrtNorskPoengaar] = useState<number | undefined>(undefined)

  useEffect(() => {
    setOverstyrtNorskPoengaar(trygdetid.overstyrtNorskPoengaar ?? undefined)
  }, [])

  const opptjentTekst = () => {
    return virkningstidspunktEtterNyRegelDato
      ? 'men har minimum tre poengår'
      : 'men har opptjent rett til tilleggspensjon'
  }

  const eksportBeskrivelse = () => {
    switch (sakType) {
      case SakType.BARNEPENSJON:
        return `Poengår skal registreres kun ved eksportvurdering når pensjonen er innvilget etter unntaksregelen om
              at avdød har mindre enn 20 års botid, ${opptjentTekst()}. Når poengår registreres, blir trygdetiden
              beregnet til antall poengår.`
      case SakType.OMSTILLINGSSTOENAD:
        return `Poengår skal registreres kun ved eksportvurdering når stønaden er innvilget etter unntaksregelen om
        at avdød har mindre enn 20 års botid, men har minimum tre poengår. Når poengår registreres, blir trygdetiden
        beregnet til antall poengår.`
    }
  }

  return (
    <Overstyrt>
      {(redigerbar || overstyrtNorskPoengaar !== undefined) && (
        <HStack gap="2">
          <Heading size="small" level="4">
            Poengår i Norge - registreres kun ved eksportvurdering
          </Heading>
        </HStack>
      )}
      {!redigerbar && overstyrtNorskPoengaar !== undefined && (
        <>
          <BodyShort>{eksportBeskrivelse()}</BodyShort>
          <AntallAar>{trygdetid.overstyrtNorskPoengaar} år</AntallAar>
        </>
      )}
      {redigerbar && (
        <>
          <BodyShort>{eksportBeskrivelse()}</BodyShort>
          <AntallAarFelt
            value={overstyrtNorskPoengaar ?? ''}
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            label="Antall år"
            onChange={(e) => setOverstyrtNorskPoengaar(e.target.value === '' ? undefined : Number(e.target.value))}
            onBlur={() => overstyrTrygdetidPoengaar({ ...trygdetid, overstyrtNorskPoengaar: overstyrtNorskPoengaar })}
          />
          <Box paddingBlock="4 0" paddingInline="0 4">
            <Button
              size="small"
              onClick={(event) => {
                event.preventDefault()
                setOverstyrtNorskPoengaar(undefined)
                overstyrTrygdetidPoengaar({ ...trygdetid, overstyrtNorskPoengaar: undefined })
              }}
              disabled={overstyrtNorskPoengaar === undefined}
            >
              Slett
            </Button>
          </Box>
        </>
      )}
    </Overstyrt>
  )
}

const Overstyrt = styled.div`
  padding: 2em 0 0 0;
`

const AntallAar = styled(BodyShort)`
  padding: 1em 0 0 0;
`

const AntallAarFelt = styled(TextField)`
  padding: 1em 0 0 0;
  width: 10em;
`
