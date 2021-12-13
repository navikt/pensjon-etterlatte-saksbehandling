export const epochToUTC = (epochDate: number): Date => {
    const date = new Date(0);
    date.setUTCSeconds(epochDate);
    return date;
};

export const utcSecondsSinceEpoch = (): number => {
    const now = new Date();
    const utcMilllisecondsSinceEpoch = now.getTime() + now.getTimezoneOffset() * 60 * 1000;
    const utcSecondsSinceEpoch = Math.round(utcMilllisecondsSinceEpoch / 1000);
    return utcSecondsSinceEpoch;
};
