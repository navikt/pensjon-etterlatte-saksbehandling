export const INVALID_FNR = (input: string | undefined) => !/^\d{11}$/.test(input ?? '')
