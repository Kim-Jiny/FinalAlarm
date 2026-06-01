// 비트마스크: 월=1(bit0), 화=2(bit1), 수=4(bit2), 목=8(bit3), 금=16(bit4), 토=32(bit5), 일=64(bit6)

export const DOW = {
  MON: 1 << 0,
  TUE: 1 << 1,
  WED: 1 << 2,
  THU: 1 << 3,
  FRI: 1 << 4,
  SAT: 1 << 5,
  SUN: 1 << 6,
} as const;

export const ALL_DAYS = 0b1111111; // 127

export function isDayActive(mask: number, isoDow: number): boolean {
  // isoDow: 1=Mon ... 7=Sun → 비트 위치 0..6
  const bit = 1 << (isoDow - 1);
  return (mask & bit) !== 0;
}

export function validateDowMask(mask: number): boolean {
  return Number.isInteger(mask) && mask > 0 && mask <= ALL_DAYS;
}
