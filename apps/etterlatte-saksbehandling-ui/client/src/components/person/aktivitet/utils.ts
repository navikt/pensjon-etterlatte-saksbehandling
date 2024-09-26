import {
  AktivitetspliktUnntakType,
  AktivitetspliktVurderingType,
  IAktivitetspliktAktivitetsgrad,
  IAktivitetspliktUnntak,
  IAktivitetspliktVurderingNy,
} from '~shared/types/Aktivitetsplikt'

export enum AktivitetspliktStatus {
  OPPFYLT,
  IKKE_OPPFYLT,
  UNNTAK,
  VARIG_UNNTAK,
  IKKE_VURDERT,
}

interface StatusPaaAktivitetsplikt {
  aktivitetspliktStatus: AktivitetspliktStatus
  dato?: Date | string
}

const sisteGjeldendeUnntak = (unntak: IAktivitetspliktUnntak[]): IAktivitetspliktUnntak => {
  return unntak.reduce((a, b) => {
    return new Date(a.fom) > new Date(b.fom) ? a : b
  })
}

const sisteGjeldendeAktivitet = (aktivitet: IAktivitetspliktAktivitetsgrad[]): IAktivitetspliktAktivitetsgrad => {
  return aktivitet.reduce((a, b) => {
    return new Date(a.fom) > new Date(b.fom) ? a : b
  })
}

export const finnStatusPaaAktivitetsplikt = (
  aktivitetspliktVurdering: IAktivitetspliktVurderingNy
): StatusPaaAktivitetsplikt => {
  // Hvis innbygger har unntak, så viker dette for aktivitetsplikt
  if (!!aktivitetspliktVurdering.unntak?.length) {
    const gjeldendeUnntak = sisteGjeldendeUnntak([...aktivitetspliktVurdering.unntak])

    if (gjeldendeUnntak.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT) {
      return {
        aktivitetspliktStatus: AktivitetspliktStatus.VARIG_UNNTAK,
      }
    } else {
      return {
        aktivitetspliktStatus: AktivitetspliktStatus.UNNTAK,
        dato: gjeldendeUnntak?.tom,
      }
    }
  } else if (!!aktivitetspliktVurdering.aktivitet?.length) {
    const gjeldendeAktivitet = sisteGjeldendeAktivitet([...aktivitetspliktVurdering.aktivitet])

    if (gjeldendeAktivitet?.aktivitetsgrad === AktivitetspliktVurderingType.AKTIVITET_UNDER_50) {
      return {
        aktivitetspliktStatus: AktivitetspliktStatus.IKKE_OPPFYLT,
        dato: gjeldendeAktivitet.fom,
      }
    } else {
      return {
        aktivitetspliktStatus: AktivitetspliktStatus.OPPFYLT,
        dato: gjeldendeAktivitet?.fom,
      }
    }
  }

  return {
    aktivitetspliktStatus: AktivitetspliktStatus.IKKE_VURDERT,
  }
}
