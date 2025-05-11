import { registerPlugin } from '@capacitor/core';

import type { PedometerPlugin } from './definitions';

const Pedometer = registerPlugin<PedometerPlugin>('Pedometer');
export * from './definitions';
export { Pedometer };
