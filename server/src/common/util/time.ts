export const TIME_OF_DAY_REGEX = /^([01]\d|2[0-3]):[0-5]\d$/;

export function isValidTimeOfDay(s: string): boolean {
  return TIME_OF_DAY_REGEX.test(s);
}

export function parseTimeOfDay(s: string): { h: number; m: number } {
  const [h, m] = s.split(':').map((x) => Number(x));
  return { h, m };
}
