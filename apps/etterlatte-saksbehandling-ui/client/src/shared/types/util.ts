// På grunn av måten React håndterer controlled vs uncontrolled inputs tror React at input med value=undefined
// er uncontrolled input. Dette stemmer dårlig med hvordan vi vil bruke skjemaet med f.eks. Partial<FilledFormData>,
// FieldOrNull gir oss en type der vi kan eksplisitt holde styr på hva som er definert eller ikke
export type FieldOrNull<T> = {
  [P in keyof T]: T[P] | null
}
