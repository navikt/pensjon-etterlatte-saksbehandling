export const GYLDIG_FNR = (input: string | undefined) => /^\d{11}$/.test(input ?? '')
