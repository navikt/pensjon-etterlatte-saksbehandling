export const epochToUTC = (epochDate: number): Date => {
    const date = new Date(0);
    date.setUTCSeconds(epochDate);
    return date;
};

export const utcSecondsSinceEpoch = (): number => {
    const now = new Date();
    const utcMilllisecondsSinceEpoch = now.getTime();
    const utcSecondsSinceEpoch = Math.round(utcMilllisecondsSinceEpoch / 1000);
    return utcSecondsSinceEpoch;
};
